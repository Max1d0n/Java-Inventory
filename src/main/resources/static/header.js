class AppHeader extends HTMLElement {
    connectedCallback() {
        const type = this.getAttribute('type') || 'main'; // 'main' або 'back'
        const title = this.getAttribute('title') || 'Inventory';
        const backUrl = this.getAttribute('back') || '/index.html';

        if (type === 'back') {
            this.innerHTML = `
                <header>
                    <div class="header-left">
                        <button class="header-btn" onclick="window.location.href='${backUrl}'">←</button>
                        <span class="header-title">${title}</span>
                    </div>
                </header>
            `;
        } else {
            this.innerHTML = `
                <header>
                    <div class="header-left" style="position:relative;">
                        <button class="header-btn" onclick="toggleLeftMenu()">☰</button>
                        <div class="left-menu" id="left-menu">
                            <div class="theme-switch-container">
                                <span>🌙 Dark Mode</span>
                                <label class="switch">
                                    <input type="checkbox" id="theme-checkbox">
                                    <span class="slider"></span>
                                </label>
                            </div>
                            
                            <a id="admin-link" href="/admin.html">🛡️ Admin Panel</a>
                        </div>
                        <span class="header-title">${title}</span>
                    </div>
                    <div class="user-menu-wrapper">
                        <button class="user-btn" onclick="toggleUserMenu()">
                            <div class="avatar" id="header-avatar">?</div>
                            <span id="header-username"></span>
                        </button>
                        <div class="dropdown-menu" id="user-menu">
                            <a href="/profile.html">👤 Profile</a>                          
                            <div class="menu-divider"></div>
                            <a class="logout-link" onclick="headerLogout()">🚪 Logout</a>
                        </div>
                    </div>
                </header>
            `;

            // Заповнюємо дані користувача
            const username = localStorage.getItem('username');
            const role = localStorage.getItem('role');

            const avatarEl = document.getElementById('header-avatar');
            const usernameEl = document.getElementById('header-username');
            const adminLink = document.getElementById('admin-link');

            if (avatarEl && username) avatarEl.textContent = username[0].toUpperCase();
            if (usernameEl && username) usernameEl.textContent = username;
            if (adminLink && role !== 'ROLE_ADMIN') adminLink.style.display = 'none';

            // ЛОГИКА СЛАЙДЕРА ТЕМЫ
            const themeCheckbox = this.querySelector('#theme-checkbox');
            if (themeCheckbox) {
                // Синхронизируем положение слайдера с текущей темой страницы
                const currentTheme = document.documentElement.getAttribute('data-theme');
                themeCheckbox.checked = currentTheme === 'dark';

                // Слушаем клик по слайдеру
                themeCheckbox.addEventListener('change', (e) => {
                    const targetTheme = e.target.checked ? 'dark' : 'light';

                    document.documentElement.setAttribute('data-theme', targetTheme);
                    localStorage.setItem('theme', targetTheme);
                });
            }
        }

        // Закриваємо меню при кліку поза ними
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.user-menu-wrapper')) {
                const menu = document.getElementById('user-menu');
                if (menu) menu.classList.remove('open');
            }
            if (!e.target.closest('.header-left')) {
                const menu = document.getElementById('left-menu');
                if (menu) menu.classList.remove('open');
            }
        });
    }
}

customElements.define('app-header', AppHeader);

function toggleUserMenu() {
    const menu = document.getElementById('user-menu');
    if (menu) menu.classList.toggle('open');
}

function toggleLeftMenu() {
    const menu = document.getElementById('left-menu');
    if (menu) menu.classList.toggle('open');
}

function headerLogout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    window.location.href = '/login.html';
}