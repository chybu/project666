const express = require('express');
const cors = require('cors');
const app = express();
app.use(cors({ origin: 'http://localhost:8080' }));
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// ← only these URIs are ever allowed
const ALLOWED_REDIRECT_URIS = [
  'http://localhost:8080/callback.html',
];

app.post('/token', async (req, res) => {
  const redirectUri = req.body.redirect_uri;

  if (!redirectUri || !ALLOWED_REDIRECT_URIS.includes(redirectUri)) {
    console.warn('Blocked invalid redirect_uri:', redirectUri);
    return res.status(400).json({ error: 'invalid_redirect_uri' });
  }

  const params = new URLSearchParams({
    grant_type:    'authorization_code',
    client_id:     'api-testing',
    client_secret: process.env.CLIENT_SECRET,
    redirect_uri:  redirectUri,
    code:          req.body.code,
  });

  console.log('--- Token Request ---');
  console.log('redirect_uri:', redirectUri);
  console.log('code:', req.body.code);
  console.log('client_secret set:', !!process.env.CLIENT_SECRET);

  const response = await fetch(
    'http://keycloak:8080/realms/patient-portal/protocol/openid-connect/token',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params,
    }
  );

  const data = await response.json();
  console.log('--- Keycloak Response ---');
  console.log('status:', response.status);
  console.log('body:', JSON.stringify(data));

  res.status(response.status).json(data);
});

app.listen(3001, () => console.log('Proxy running on 3001'));