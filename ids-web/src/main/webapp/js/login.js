'use strict';
(() => {
  if (IDS.sesion()) { location.replace('dashboard.html'); return; }
  const msg = document.getElementById('msg');
  if (IDS.parametro('expirada')) {
    msg.textContent = 'Tu sesión expiró. Inicia sesión de nuevo.';
    msg.classList.remove('d-none');
  }
  document.getElementById('formLogin').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    const btn = document.getElementById('btnEntrar');
    btn.disabled = true;
    msg.classList.add('d-none');
    try {
      const r = await IDS.post('/auth/login', {
        email: document.getElementById('email').value.trim(),
        password: document.getElementById('password').value
      });
      IDS.guardarSesion(r);
      location.replace('dashboard.html');
    } catch (e) {
      msg.textContent = e.message;
      msg.classList.remove('d-none');
      msg.classList.replace('alert-warning', 'alert-danger');
    } finally {
      btn.disabled = false;
    }
  });
})();
