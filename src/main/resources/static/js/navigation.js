// Presentation-only navigation: the existing application actions stay in app.js.
const menuToggle = document.querySelector('#menu-toggle');
const navigation = document.querySelector('#app-navigation');
const narrowLayout = window.matchMedia('(max-width: 959px)');

function setMenuOpen(open, restoreFocus = false) {
  navigation.classList.toggle('is-open', open);
  menuToggle.setAttribute('aria-expanded', String(open));
  menuToggle.setAttribute('aria-label', open ? 'Fechar menu' : 'Abrir menu');
  navigation.inert = narrowLayout.matches && !open;
  if (restoreFocus) menuToggle.focus();
}

menuToggle.addEventListener('click', () => {
  const open = menuToggle.getAttribute('aria-expanded') !== 'true';
  setMenuOpen(open);
});
menuToggle.addEventListener('keydown', event => {
  if (event.key === 'Tab' && !event.shiftKey && menuToggle.getAttribute('aria-expanded') === 'true') {
    event.preventDefault();
    navigation.querySelector('.nav-item').focus({ preventScroll: true });
  }
});
navigation.querySelectorAll('.nav-item').forEach(link => {
  link.addEventListener('click', () => {
    navigation.querySelectorAll('.nav-item').forEach(item => {
      item.classList.toggle('is-active', item === link);
      if (item === link) item.setAttribute('aria-current', 'location');
      else item.removeAttribute('aria-current');
    });
    if (narrowLayout.matches) {
      setMenuOpen(false);
      const section = document.querySelector(link.getAttribute('href'));
      section.setAttribute('tabindex', '-1');
      section.focus({ preventScroll: true });
    }
  });
});
navigation.addEventListener('keydown', event => {
  if (!narrowLayout.matches || event.key !== 'Tab' || event.shiftKey) return;
  const links = navigation.querySelectorAll('.nav-item');
  if (event.target === links[links.length - 1]) {
    event.preventDefault();
    setMenuOpen(false);
    document.querySelector('#profile-button').focus();
  }
});
document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && menuToggle.getAttribute('aria-expanded') === 'true') setMenuOpen(false, true);
});
document.addEventListener('click', event => {
  if (narrowLayout.matches && !navigation.contains(event.target) && !menuToggle.contains(event.target)) setMenuOpen(false);
});
narrowLayout.addEventListener('change', () => setMenuOpen(false));
setMenuOpen(false);
