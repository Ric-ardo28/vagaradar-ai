const cookieValue = name => document.cookie.split('; ').find(row => row.startsWith(`${name}=`))?.split('=').slice(1).join('=');
const csrfToken = cookieValue('XSRF-TOKEN');
if (csrfToken) document.querySelector('#csrf-token').value = decodeURIComponent(csrfToken);

const query = new URLSearchParams(window.location.search);
if (query.has('error')) document.querySelector('#login-error').hidden = false;
if (query.get('session') === 'renewed') document.querySelector('#login-session-renewed').hidden = false;
