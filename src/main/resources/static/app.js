(() => {
  const $ = (selector) => document.querySelector(selector);
  const state = { token: localStorage.getItem('cookieToken'), playerId: localStorage.getItem('cookiePlayerId'), username: localStorage.getItem('cookieUsername'), mode: 'login', player: null, upgrades: [] };
  const authScreen = $('#auth-screen'), gameScreen = $('#game-screen');
  const number = (n) => new Intl.NumberFormat('fr-FR').format(n);
  let toastTimer;

  async function api(path, options = {}) {
    const headers = { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}), ...options.headers };
    const response = await fetch(path, { ...options, headers });
    if (response.status === 204) return null;
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      if (response.status === 401 && state.token) logout(false);
      throw new Error(data.message || 'Une erreur est survenue. Réessayez.');
    }
    return data;
  }
  function notify(message) { const toast = $('#toast'); toast.textContent = message; toast.classList.add('show'); clearTimeout(toastTimer); toastTimer = setTimeout(() => toast.classList.remove('show'), 2500); }
  function showAuth(message = '') { authScreen.classList.remove('hidden'); gameScreen.classList.add('hidden'); $('#logout').classList.add('hidden'); $('#account-name').textContent = 'Invité'; $('#auth-message').textContent = message; }
  function showGame() { authScreen.classList.add('hidden'); gameScreen.classList.remove('hidden'); $('#logout').classList.remove('hidden'); $('#account-name').textContent = state.username || 'Joueur'; }
  function logout(show = true) { state.token = null; state.playerId = null; state.username = null; localStorage.removeItem('cookieToken'); localStorage.removeItem('cookiePlayerId'); localStorage.removeItem('cookieUsername'); if (show) showAuth(); }
  function iconFor(name) { const icons = ['🖱️','🥣','🧑‍🍳','🔥','🏭','🚀']; return icons[Math.abs([...name].reduce((sum, char) => sum + char.charCodeAt(0), 0)) % icons.length]; }

  async function refresh() {
    $('#status').textContent = 'Synchronisation…';
    const [player, upgrades, ranking] = await Promise.all([api(`/api/players/${state.playerId}`), api('/api/upgrades'), api('/api/players/leaderboard')]);
    state.player = player; state.upgrades = upgrades;
    renderPlayer(); renderUpgrades(); renderRanking(ranking);
    $('#status').textContent = 'Progression sauvegardée';
  }
  function renderPlayer() {
    $('#cookies').textContent = number(state.player.cookies);
    $('#per-click').textContent = number(state.player.cookiesPerClick);
    $('#per-second').textContent = number(state.player.cookiesPerSecond);
  }
  function renderUpgrades() {
    $('#shop-count').textContent = `${state.upgrades.length} objet${state.upgrades.length > 1 ? 's' : ''}`;
    const list = $('#upgrades');
    if (!state.upgrades.length) { list.innerHTML = '<div class="empty-state">Aucune amélioration disponible pour le moment.</div>'; return; }
    list.innerHTML = state.upgrades.map((item) => {
      const afford = state.player.cookies >= item.price;
      const bonus = [item.clickBonus ? `+${number(item.clickBonus)} / clic` : '', item.passiveBonus ? `+${number(item.passiveBonus)} / sec` : ''].filter(Boolean).join(' · ');
      return `<article class="upgrade"><div class="upgrade-icon">${iconFor(item.name)}</div><div><h3>${escapeHtml(item.name)}</h3><p>${escapeHtml(item.description || bonus)}</p></div><button class="buy" data-upgrade="${item.id}" ${afford ? '' : 'disabled'}>${number(item.price)} 🍪<span>${bonus}</span></button></article>`;
    }).join('');
    list.querySelectorAll('[data-upgrade]').forEach((button) => button.addEventListener('click', () => buy(button.dataset.upgrade)));
  }
  function renderRanking(rows) {
    const root = $('#leaderboard');
    root.innerHTML = rows.length ? rows.map((row, index) => `<div class="rank-row ${Number(row.id) === Number(state.playerId) ? 'mine' : ''}"><span class="rank-no">${String(index + 1).padStart(2, '0')}</span><span class="rank-name">${escapeHtml(row.username)}${Number(row.id) === Number(state.playerId) ? ' · vous' : ''}</span><span class="rank-cookies">${number(row.cookies)} 🍪</span></div>`).join('') : '<div class="empty-state">Le classement se remplit à la première fournée.</div>';
  }
  function escapeHtml(value) { return String(value).replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]); }
  async function clickCookie() {
    const button = $('#cookie'); button.animate([{ transform: 'scale(1)' }, { transform: 'scale(.92)' }, { transform: 'scale(1)' }], { duration: 130 });
    try { state.player = await api(`/api/players/${state.playerId}/click`, { method: 'POST' }); renderPlayer(); renderUpgrades(); }
    catch (error) { notify(error.message); }
  }
  async function buy(upgradeId) {
    try { state.player = await api(`/api/players/${state.playerId}/upgrades/${upgradeId}/purchase`, { method: 'POST' }); renderPlayer(); renderUpgrades(); notify('Amélioration achetée !'); }
    catch (error) { notify(error.message); }
  }

  document.querySelectorAll('.tab').forEach((tab) => tab.addEventListener('click', () => {
    state.mode = tab.dataset.mode; document.querySelectorAll('.tab').forEach((item) => item.classList.toggle('active', item === tab));
    $('#auth-submit').innerHTML = state.mode === 'login' ? 'Entrer dans le jeu <span>→</span>' : 'Créer mon compte <span>→</span>';
    $('#password').autocomplete = state.mode === 'login' ? 'current-password' : 'new-password'; $('#auth-message').textContent = '';
  }));
  $('#auth-form').addEventListener('submit', async (event) => {
    event.preventDefault(); const button = $('#auth-submit'); button.disabled = true; $('#auth-message').textContent = '';
    const payload = { username: $('#username').value.trim(), password: $('#password').value };
    try {
      const result = await api(`/api/auth/${state.mode === 'login' ? 'login' : 'signup'}`, { method: 'POST', body: JSON.stringify(payload) });
      state.token = result.accessToken; state.playerId = result.playerId; state.username = result.username;
      localStorage.setItem('cookieToken', state.token); localStorage.setItem('cookiePlayerId', state.playerId); localStorage.setItem('cookieUsername', state.username);
      showGame(); await refresh();
    } catch (error) { $('#auth-message').textContent = error.message; }
    finally { button.disabled = false; }
  });
  $('#cookie').addEventListener('click', clickCookie);
  $('#logout').addEventListener('click', () => logout());
  $('#refresh').addEventListener('click', () => refresh().catch((error) => notify(error.message)));

  window.setInterval(async () => {
    if (!state.token || !state.player || state.player.cookiesPerSecond < 1) return;
    try { state.player = await api(`/api/players/${state.playerId}/collect`, { method: 'POST' }); renderPlayer(); renderUpgrades(); }
    catch (error) { if (state.token) notify(error.message); }
  }, 1000);

  if (state.token && state.playerId) { showGame(); refresh().catch(() => showAuth('Session expirée. Connectez-vous à nouveau.')); }
  else showAuth();
})();
