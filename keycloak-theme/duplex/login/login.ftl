<!DOCTYPE html>
<html lang="${(locale.currentLanguageTag)!'en'}">
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
        }

        .login-wrapper {
            width: 100%;
            max-width: 420px;
            padding: 1rem;
        }

        .login-card {
            background: #fff;
            border-radius: 12px;
            box-shadow: 0 0.5rem 2rem rgba(58, 59, 69, 0.15);
            padding: 2.5rem 2rem;
        }

        /* Header */
        .login-header {
            text-align: center;
            margin-bottom: 2rem;
        }

        .login-logo {
            height: 64px;
            width: auto;
            margin-bottom: 0.75rem;
        }

        .login-subtitle {
            color: #4e73df;
            font-size: 0.8rem;
            font-weight: 700;
            letter-spacing: 0.12em;
            text-transform: uppercase;
        }

        /* Alerts */
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

        /* Form */
        .form-group { margin-bottom: 1.25rem; }

        .form-group label {
            display: block;
            font-size: 0.78rem;
            font-weight: 700;
            color: #5a5c69;
            margin-bottom: 0.4rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        .form-control {
            width: 100%;
            padding: 0.6rem 0.85rem;
            border: 1px solid #e3e6f0;
            border-radius: 6px;
            font-size: 0.9rem;
            font-family: inherit;
            color: #3a3b45;
            background: #fff;
            transition: border-color 0.2s, box-shadow 0.2s;
            outline: none;
        }
        .form-control:focus {
            border-color: #4e73df;
            box-shadow: 0 0 0 3px rgba(78, 115, 223, 0.15);
        }

        .field-error {
            font-size: 0.78rem;
            color: #e74a3b;
            margin-top: -0.75rem;
            margin-bottom: 1rem;
        }

        /* Options row */
        .form-options {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 1.5rem;
            font-size: 0.85rem;
        }

        .checkbox-label {
            display: flex;
            align-items: center;
            gap: 0.4rem;
            cursor: pointer;
            color: #5a5c69;
            font-weight: 500;
        }
        .checkbox-label input[type="checkbox"] {
            accent-color: #4e73df;
            width: 15px;
            height: 15px;
            cursor: pointer;
        }

        .forgot-link {
            color: #4e73df;
            text-decoration: none;
            font-weight: 600;
        }
        .forgot-link:hover { color: #2e59d9; text-decoration: underline; }

        /* Submit */
        .btn-login {
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
        }
        .btn-login:hover    { background: #2e59d9; }
        .btn-login:active   { background: #2653d4; }
        .btn-login:disabled { background: #858796; cursor: not-allowed; }

        /* Password wrapper with eye toggle */
        .password-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }
        .password-wrapper .form-control {
            padding-right: 2.5rem;
        }
        .btn-eye {
            position: absolute;
            right: 0.6rem;
            background: none;
            border: none;
            cursor: pointer;
            padding: 0;
            display: flex;
            align-items: center;
            color: #858796;
            transition: color 0.2s;
        }
        .btn-eye:hover { color: #4e73df; }
        .btn-eye svg { width: 18px; height: 18px; }

        /* Footer */
        .login-footer {
            text-align: center;
            margin-top: 1.5rem;
            font-size: 0.85rem;
            color: #858796;
        }
        .login-footer a {
            color: #4e73df;
            font-weight: 700;
            text-decoration: none;
            margin-left: 0.25rem;
        }
        .login-footer a:hover { color: #2e59d9; text-decoration: underline; }
    </style>
</head>
<body>
    <div class="login-wrapper">
        <div class="login-card">

            <!-- Logo & Title -->
            <div class="login-header">
                <img src="${url.resourcesPath}/img/logo.png" alt="Duplex" class="login-logo">
                <p class="login-subtitle">Patient Portal</p>
            </div>

            <!-- Alert Messages -->
            <#if message?has_content>
            <div class="alert alert-${message.type}">
                ${kcSanitize(message.summary)?no_esc}
            </div>
            </#if>

            <!-- Login Form -->
            <form id="kc-form-login" action="${url.loginAction}" method="post">

                <div class="form-group">
                    <label for="username">
                        <#if !realm.loginWithEmailAllowed>
                            ${msg("username")}
                        <#elseif !realm.registrationEmailAsUsername>
                            ${msg("usernameOrEmail")}
                        <#else>
                            ${msg("email")}
                        </#if>
                    </label>
                    <input type="text"
                           id="username"
                           name="username"
                           class="form-control"
                           value="${(login.username!'')}"
                           autofocus
                           autocomplete="off">
                </div>

                <div class="form-group">
                    <label for="password">${msg("password")}</label>
                    <div class="password-wrapper">
                        <input type="password"
                               id="password"
                               name="password"
                               class="form-control"
                               autocomplete="current-password">
                        <button type="button" class="btn-eye" id="toggle-password" aria-label="Show password" tabindex="-1">
                            <svg id="eye-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18">
                                <path fill="#858796" d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
                            </svg>
                            <svg id="eye-off-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="18" height="18" style="display:none">
                                <path fill="#858796" d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75C21.27 7.61 17 4.5 12 4.5c-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/>
                            </svg>
                        </button>
                    </div>
                </div>

                <#if message?has_content && message.type = 'error'>
                    <p class="field-error">${kcSanitize(message.summary)?no_esc}</p>
                </#if>

                <div class="form-options">
                    <#if realm.rememberMe && !usernameEditDisabled??>
                        <label class="checkbox-label">
                            <input type="checkbox"
                                   id="rememberMe"
                                   name="rememberMe"
                                   <#if login.rememberMe??>checked</#if>>
                            ${msg("rememberMe")}
                        </label>
                    <#else>
                        <span></span>
                    </#if>
                    <#if realm.resetPasswordAllowed>
                        <a href="${url.loginResetCredentialsUrl}" class="forgot-link">${msg("doForgotPassword")}</a>
                    </#if>
                </div>

                <input type="hidden" id="id-hidden-input" name="credentialId"
                       <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>>

                <button type="submit" id="kc-login" name="login" class="btn-login">
                    ${msg("doLogIn")}
                </button>

            </form>

            <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div class="login-footer">
                <span>${msg("noAccount")}</span>
                <a href="${url.registrationUrl}">${msg("doRegister")}</a>
            </div>
            </#if>

        </div>
    </div>
    <script>
        (function() {
            var btn = document.getElementById('toggle-password');
            var input = document.getElementById('password');
            var eyeOn = document.getElementById('eye-icon');
            var eyeOff = document.getElementById('eye-off-icon');
            if (btn && input) {
                btn.addEventListener('click', function() {
                    var isPassword = input.type === 'password';
                    input.type = isPassword ? 'text' : 'password';
                    eyeOn.style.display = isPassword ? 'none' : '';
                    eyeOff.style.display = isPassword ? '' : 'none';
                });
            }
        })();
    </script>
</body>
</html>
