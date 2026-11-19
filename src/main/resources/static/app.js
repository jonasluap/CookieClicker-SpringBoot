(() => {
  const $ = (selector) => document.querySelector(selector);
  const authButton = $('#auth-submit');
  // Spring Boot serves both the page and its API. Opening the HTML file directly has no API.
  if (window.location.protocol === 'file:') {
    authButton.disabled = true;
    $('#auth-message').textContent = 'Démarrez Spring Boot puis ouvrez http://localhost:8082/ avec le profil local, ou http://localhost:8080/ avec Docker.';
    return;
  }

  // Restore the token across reloads. The server supplies the current player and catalogue.
  const state = { token: localStorage.getItem('cookieToken'), mode: 'login', player: null, upgrades: [] };
  const authScreen = $('#auth-screen'), gameScreen = $('#game-screen');
  const number = (n) => new Intl.NumberFormat('fr-FR').format(n);
  const connectionMessage = 'Impossible de joindre le jeu. Vérifiez que Spring Boot est démarré et ouvrez son adresse.';
  let toastTimer;
  let gameRequests = Promise.resolve();
  let collecting = false;

  // Send requests to the same origin and turn API errors into messages for the current screen.
  async function api(path, options, token) {
    const headers = { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) };
    let response;
    try {
      response = await fetch(`api/${path}`, { ...options, headers });
    } catch {
      throw new Error(connectionMessage);
    }
    // A late response from an old session must leave the current session alone.
    if (response.status === 401 && token && token === state.token) {
      logout('Session expirée. Connectez-vous à nouveau.');
      throw new Error('Session expirée.');
    }
    if (response.status === 204) return null;
    if (!response.headers.get('content-type')?.includes('application/json')) {
      throw new Error(connectionMessage);
    }
    const data = await response.json();
    if (!response.ok) {
      if (response.status === 401) throw new Error('Nom de joueur ou mot de passe incorrect.');
      throw new Error(data.message || 'Une erreur est survenue. Réessayez.');
    }
    return data;
  }
  // Screen transitions and notifications.
  function notify(message) { const toast = $('#toast'); toast.textContent = message; toast.classList.add('show'); clearTimeout(toastTimer); toastTimer = setTimeout(() => toast.classList.remove('show'), 2500); }
  function showAuth(message = '') { authScreen.classList.remove('hidden'); gameScreen.classList.add('hidden'); $('#logout').classList.add('hidden'); $('#account-name').textContent = 'Invité'; $('#auth-message').textContent = message; }
  function showGame() {
    authScreen.classList.add('hidden');
    gameScreen.classList.remove('hidden');
    $('#logout').classList.remove('hidden');
    $('#account-name').textContent = 'Joueur';
    // Keep gameplay controls disabled until the profile has finished loading.
    $('#cookie').disabled = true;
    $('#cookies').textContent = '–';
    $('#per-click').textContent = '–';
    $('#per-second').textContent = '–';
    $('#shop-count').textContent = '–';
    $('#upgrades').textContent = 'Chargement des améliorations…';
    $('#leaderboard').replaceChildren();
    $('#weather-form').reset();
    $('.weather-button').disabled = true;
    $('#weather-message').textContent = '';
    renderWeather(null);
    refresh();
  }

  function logout(message = '') {
    state.token = null;
    state.player = null;
    state.upgrades = [];
    localStorage.removeItem('cookieToken');
    localStorage.removeItem('cookiePlayerId');
    localStorage.removeItem('cookieUsername');
    showAuth(message);
  }

  function iconFor(name) { const icons = ['🖱️','🥣','🧑‍🍳','🔥','🏭','🚀']; return icons[Math.abs([...name].reduce((sum, char) => sum + char.charCodeAt(0), 0)) % icons.length]; }

  // Weather codes select the icon and label. The API supplies the bonus and whether it has expired.
  const weatherDescriptions = {
    0: ['☀️', 'Ciel dégagé — parfait pour une fournée ensoleillée.'],
    1: ['🌤️', 'Principalement dégagé — la journée s’annonce douce.'],
    2: ['⛅', 'Quelques nuages — idéal pour une pause goûter.'],
    3: ['☁️', 'Ciel couvert — le four réchauffe l’ambiance.'],
    45: ['🌫️', 'Brouillard — cookies réconfortants recommandés.'], 48: ['🌫️', 'Brouillard givrant — restez au chaud.'],
    51: ['🌦️', 'Bruine légère — parfait pour rester pâtisser.'], 53: ['🌦️', 'Bruine — une fournée mettra du soleil.'], 55: ['🌧️', 'Bruine dense — journée idéale pour les cookies.'],
    56: ['🌧️', 'Bruine verglaçante légère — restez bien au chaud.'], 57: ['🌧️', 'Bruine verglaçante — un chocolat chaud s’impose.'],
    61: ['🌧️', 'Pluie légère — parfait pour une pause gourmande.'], 63: ['🌧️', 'Pluie — le four devient votre meilleur ami.'], 65: ['🌧️', 'Forte pluie — journée parfaite pour les cookies.'],
    66: ['🌧️', 'Pluie verglaçante légère — pâtissez au chaud.'], 67: ['🌧️', 'Pluie verglaçante — restez à l’abri.'],
    71: ['🌨️', 'Quelques flocons — sortez les cookies tout chauds.'], 73: ['🌨️', 'Neige — une fournée bien chaude s’impose.'], 75: ['❄️', 'Forte neige — restez au chaud avec vos cookies.'], 77: ['🌨️', 'Grains de neige — ambiance cocooning.'],
    80: ['🌦️', 'Averses passagères — profitez de l’accalmie pour pâtisser.'], 81: ['🌧️', 'Averses — le four tourne à plein régime.'], 82: ['⛈️', 'Fortes averses — journée cocooning.'],
    85: ['🌨️', 'Averses de neige — cookies tout juste sortis du four.'], 86: ['❄️', 'Fortes averses de neige — restez bien au chaud.'],
    95: ['⛈️', 'Orage — mieux vaut rester à l’intérieur.'], 96: ['🌩️', 'Orage et grêle — le bon moment pour une pause.'], 99: ['🌩️', 'Orage violent et grêle — restez à l’abri.']
  };

  function renderWeather(weather) {
    const [icon, description] = weather
      ? weatherDescriptions[weather.code] || ['🌡️', 'Conditions météo du moment.']
      : ['🌤️', 'Recherchez une ville pour activer un bonus de production.'];
    $('#weather-icon').textContent = icon;
    $('#weather-place').textContent = weather?.place ?? 'Choisissez une ville';
    $('#weather-description').textContent = description;
    $('#weather-temperature').textContent = weather ? `${Math.round(weather.temperature)}°C` : '—°';
    const bonus = $('#weather-bonus');
    if (!weather) {
      bonus.textContent = '';
    } else if (weather.expired) {
      bonus.textContent = 'Bonus expiré. Recherchez à nouveau la ville pour le réactiver.';
    } else if (weather.bonus > 0) {
      bonus.textContent = `Bonus météo : +${weather.bonus} par clic et +${weather.bonus} par seconde`;
    } else {
      bonus.textContent = 'Ces conditions ne donnent pas de bonus.';
    }
  }

  // Send the city to the server, then reload the profile with its effective production rates.
  async function loadWeather(city) {
    const button = $('.weather-button');
    const message = $('#weather-message');
    if (!state.player || button.disabled) return;
    const token = state.token;
    button.disabled = true;
    message.textContent = 'Recherche…';
    try {
      await api(`players/${state.player.id}/weather`, {
        method: 'PUT',
        body: JSON.stringify({ city })
      }, token);
      if (token !== state.token) return;
      message.textContent = 'Météo mise à jour.';
      await refresh();
    } catch (error) {
      if (token === state.token) message.textContent = error.message;
    } finally {
      if (token === state.token) button.disabled = false;
    }
  }

  // Queue gameplay actions and refreshes so an older response cannot overwrite a newer one.
  function runGameRequest(action) {
    const token = state.token;
    gameRequests = gameRequests.then(async () => {
      if (!token || token !== state.token) return;
      try {
        await action(token);
      } catch (error) {
        if (token === state.token) notify(error.message);
      }
    });
    return gameRequests;
  }

  // Load the three parts of the game screen together after sign-in or on a manual refresh.
  function refresh() {
    return runGameRequest(async (token) => {
      $('#status').textContent = 'Synchronisation…';
      try {
        const [player, upgrades, ranking] = await Promise.all([
          api('players', {}, token),
          api('upgrades', {}, token),
          api('players/leaderboard', {}, token)
        ]);
        if (token !== state.token) return;
        if (!state.player) $('.weather-button').disabled = false;
        state.player = player;
        state.upgrades = upgrades;
        $('#account-name').textContent = player.username;
        $('#cookie').disabled = false;
        renderPlayer();
        renderUpgrades();
        renderRanking(ranking);
        $('#status').textContent = 'Progression sauvegardée';
      } catch (error) {
        if (token === state.token) {
          $('#status').textContent = 'Synchronisation impossible. Actualisez pour réessayer.';
          if (!state.player) $('#upgrades').textContent = 'La boutique est momentanément indisponible.';
        }
        throw error;
      }
    });
  }
  // Render the latest server state into the balance, shop and leaderboard.
  function renderPlayer() {
    $('#cookies').textContent = number(state.player.cookies);
    $('#per-click').textContent = number(state.player.cookiesPerClick);
    $('#per-second').textContent = number(state.player.cookiesPerSecond);
    renderWeather(state.player.weather);
  }
  function renderUpgrades() {
    $('#shop-count').textContent = `${state.upgrades.length} objet${state.upgrades.length > 1 ? 's' : ''}`;
    const list = $('#upgrades');
    if (!state.upgrades.length) { list.innerHTML = '<div class="empty-state">Aucune amélioration disponible pour le moment.</div>'; return; }
    list.innerHTML = state.upgrades.map((item) => {
      // Affordability here controls the button. The server checks the balance again at purchase time.
      const afford = state.player.cookies >= item.price;
      const bonus = [item.clickBonus ? `+${number(item.clickBonus)} / clic` : '', item.passiveBonus ? `+${number(item.passiveBonus)} / sec` : ''].filter(Boolean).join(' · ');
      return `<article class="upgrade"><div class="upgrade-icon">${iconFor(item.name)}</div><div><h3>${escapeHtml(item.name)}</h3><p>${escapeHtml(item.description || bonus)}</p></div><button class="buy" data-upgrade="${item.id}" ${afford ? '' : 'disabled'}>${number(item.price)} 🍪<span>${bonus}</span></button></article>`;
    }).join('');
    list.querySelectorAll('[data-upgrade]').forEach((button) => button.addEventListener('click', () => buy(button.dataset.upgrade)));
  }
  function renderRanking(rows) {
    const root = $('#leaderboard');
    root.innerHTML = rows.length ? rows.map((row, index) => `<div class="rank-row ${row.id === state.player.id ? 'mine' : ''}"><span class="rank-no">${String(index + 1).padStart(2, '0')}</span><span class="rank-name">${escapeHtml(row.username)}${row.id === state.player.id ? ' · vous' : ''}</span><span class="rank-cookies">${number(row.cookies)} 🍪</span></div>`).join('') : '<div class="empty-state">Le classement se remplit à la première fournée.</div>';
  }
  // Escape text from the API before inserting it into an HTML template.
  function escapeHtml(value) { return String(value).replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]); }

  // Clicks, purchases and passive collection all return the updated player profile.
  function updatePlayer(action, message) {
    return runGameRequest(async (token) => {
      const player = await api(`players/${state.player.id}/${action}`, { method: 'POST' }, token);
      if (token !== state.token) return;
      state.player = player;
      renderPlayer();
      renderUpgrades();
      if (message) notify(message);
    });
  }

  function clickCookie() {
    const button = $('#cookie'); button.animate([{ transform: 'scale(1)' }, { transform: 'scale(.92)' }, { transform: 'scale(1)' }], { duration: 130 });
    updatePlayer('click');
  }

  function buy(upgradeId) {
    updatePlayer(`upgrades/${upgradeId}/purchase`, 'Amélioration achetée !');
  }

  // The login and signup tabs share one form and select which authentication route to call.
  document.querySelectorAll('.tab').forEach((tab) => tab.addEventListener('click', () => {
    state.mode = tab.dataset.mode; document.querySelectorAll('.tab').forEach((item) => item.classList.toggle('active', item === tab));
    $('#auth-submit').innerHTML = state.mode === 'login' ? 'Entrer dans le jeu <span>→</span>' : 'Créer mon compte <span>→</span>';
    $('#password').autocomplete = state.mode === 'login' ? 'current-password' : 'new-password'; $('#auth-message').textContent = '';
  }));
  $('#auth-form').addEventListener('submit', async (event) => {
    event.preventDefault(); authButton.disabled = true; $('#auth-message').textContent = '';
    const payload = { username: $('#username').value.trim(), password: $('#password').value };
    try {
      const result = await api(`auth/${state.mode}`, { method: 'POST', body: JSON.stringify(payload) });
      state.token = result.accessToken;
      localStorage.setItem('cookieToken', state.token);
      $('#password').value = '';
      showGame();
    } catch (error) { $('#auth-message').textContent = error.message; }
    finally { authButton.disabled = false; }
  });
  // Bind the game controls to their API actions.
  $('#cookie').addEventListener('click', clickCookie);
  $('#logout').addEventListener('click', () => logout());
  $('#refresh').addEventListener('click', refresh);
  $('#weather-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const city = $('#weather-city').value.trim();
    if (city) loadWeather(city);
  });
  // Ask the server for one passive production tick at a time while the game page is open.
  window.setInterval(async () => {
    if (collecting || !state.player?.cookiesPerSecond) return;
    collecting = true;
    try {
      await updatePlayer('collect');
    } finally {
      collecting = false;
    }
  }, 1000);

  // A saved token is checked by the server when the game loads its data.
  if (state.token) showGame();
  else showAuth();
})();
