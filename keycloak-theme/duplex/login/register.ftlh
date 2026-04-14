<!DOCTYPE html>
<html lang="${locale.currentLanguageTag}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>Duplex Patient Portal</title>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            font-family: 'Nunito', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #f8f9fc 0%, #e8edf8 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #5a5c69;
            padding: 1.5rem 1rem;
        }

        .card {
            background: #fff;
            border-radius: 12px;
            box-shadow: 0 0.5rem 2rem rgba(58,59,69,0.15);
            padding: 2.5rem 2rem;
            width: 100%;
            max-width: 420px;
        }

        .card-header {
            text-align: center;
            margin-bottom: 1.25rem;
        }

        .card-logo {
            height: 64px;
            width: auto;
            margin-bottom: 0.75rem;
        }

        .card-subtitle {
            color: #4e73df;
            font-size: 0.8rem;
            font-weight: 700;
            letter-spacing: 0.12em;
            text-transform: uppercase;
        }

        .required-note {
            font-size: 0.75rem;
            color: #858796;
            text-align: right;
            margin-bottom: 1rem;
        }

        .alert {
            padding: 0.75rem 1rem;
            border-radius: 8px;
            font-size: 0.875rem;
            font-weight: 500;
            margin-bottom: 1.25rem;
        }
        .alert-error   { background: #fde8e6; border-left: 4px solid #e74a3b; color: #c0392b; }
        .alert-success { background: #e8f8f1; border-left: 4px solid #1cc88a; color: #0e6b4a; }
        .alert-warning { background: #fef9e7; border-left: 4px solid #f6c23e; color: #9a7d0a; }
        .alert-info    { background: #eaf4fb; border-left: 4px solid #36b9cc; color: #1a6274; }

        .form-group { margin-bottom: 1.1rem; }

        .form-group label {
            display: block;
            font-size: 0.78rem;
            font-weight: 700;
            color: #5a5c69;
            margin-bottom: 0.4rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        .req { color: #e74a3b; margin-left: 0.1rem; }

        .form-control {
            width: 100%;
            padding: 0.6rem 0.85rem;
            border: 1px solid #e3e6f0;
            border-radius: 6px;
            font-size: 0.9rem;
            font-family: inherit;
            color: #3a3b45;
            background: #fff;
            outline: none;
            transition: border-color 0.2s, box-shadow 0.2s;
        }
        .form-control:focus {
            border-color: #4e73df;
            box-shadow: 0 0 0 3px rgba(78,115,223,0.15);
        }
        .has-error { border-color: #e74a3b; }

        .field-error {
            font-size: 0.75rem;
            color: #e74a3b;
            margin-top: 0.3rem;
        }

        .pw-wrap {
            position: relative;
        }
        .pw-wrap .form-control {
            padding-right: 2.5rem;
        }
        .pw-toggle {
            position: absolute;
            top: 50%;
            right: 0.6rem;
            transform: translateY(-50%);
            background: none;
            border: none;
            padding: 0;
            cursor: pointer;
            line-height: 0;
        }

        .name-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 0.75rem;
        }

        .btn-submit {
            width: 100%;
            padding: 0.65rem;
            background: #4e73df;
            color: #fff;
            border: none;
            border-radius: 6px;
            font-size: 0.875rem;
            font-family: inherit;
            font-weight: 700;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            cursor: pointer;
            transition: background-color 0.2s;
            margin-top: 0.5rem;
        }
        .btn-submit:hover  { background: #2e59d9; }
        .btn-submit:active { background: #2653d4; }

        .back-link {
            display: block;
            text-align: center;
            margin-top: 1.25rem;
            font-size: 0.85rem;
            color: #4e73df;
            font-weight: 600;
            text-decoration: none;
        }
        .back-link:hover { color: #2e59d9; text-decoration: underline; }
    </style>
</head>
<body>
    <div class="card">

        <div class="card-header">
            <img src="${url.resourcesPath}/img/logo.png" alt="Duplex" class="card-logo">
            <p class="card-subtitle">Register</p>
        </div>

        <p class="required-note">* Required fields</p>

        <#if message?has_content>
        <div class="alert alert-${message.type}">
            ${kcSanitize(message.summary)?no_esc}
        </div>
        </#if>

        <form action="${url.registrationAction}" method="post">

            <div class="name-row">
                <div class="form-group">
                    <label for="firstName">First name <span class="req">*</span></label>
                    <input type="text" id="firstName" name="firstName" class="form-control" autofocus>
                </div>
                <div class="form-group">
                    <label for="lastName">Last name <span class="req">*</span></label>
                    <input type="text" id="lastName" name="lastName" class="form-control">
                </div>
            </div>

            <div class="form-group">
                <label for="email">Email <span class="req">*</span></label>
                <input type="email" id="email" name="email" class="form-control" autocomplete="email">
            </div>

            <#if !realm.registrationEmailAsUsername>
            <div class="form-group">
                <label for="username">Username <span class="req">*</span></label>
                <input type="text" id="username" name="username" class="form-control" autocomplete="username">
            </div>
            </#if>

            <div class="form-group">
                <label for="password">Password <span class="req">*</span></label>
                <div class="pw-wrap">
                    <input type="password" id="password" name="password" class="form-control" autocomplete="new-password">
                    <button type="button" class="pw-toggle" aria-label="Show password" onclick="togglePw('password', this)">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                            <path fill="#858796" d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                        </svg>
                    </button>
                </div>
            </div>

            <div class="form-group">
                <label for="password-confirm">Confirm password <span class="req">*</span></label>
                <div class="pw-wrap">
                    <input type="password" id="password-confirm" name="password-confirm" class="form-control" autocomplete="new-password">
                    <button type="button" class="pw-toggle" aria-label="Show confirm password" onclick="togglePw('password-confirm', this)">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                            <path fill="#858796" d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                        </svg>
                    </button>
                </div>
            </div>

            <button type="submit" class="btn-submit">Register</button>

        </form>

        <a href="${url.loginUrl}" class="back-link">&larr; Back to Login</a>

    </div>
    <script>
        var EYE_OPEN  = '<path fill="#858796" d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>';
        var EYE_OFF   = '<path fill="#858796" d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75C21.27 7.61 17 4.5 12 4.5c-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>';

        function togglePw(inputId, btn) {
            var inp = document.getElementById(inputId);
            var svg = btn.querySelector('svg');
            if (inp.type === 'password') {
                inp.type = 'text';
                svg.innerHTML = EYE_OFF;
            } else {
                inp.type = 'password';
                svg.innerHTML = EYE_OPEN;
            }
        }
    </script>
</body>
</html>
