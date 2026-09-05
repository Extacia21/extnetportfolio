// ========================================
// DOM READY
// ========================================
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

// ========================================
// APP INIT
// ========================================
function initApp() {
    // Hide loading screen
    setTimeout(() => {
        document.getElementById('loading-screen').classList.add('hidden');
    }, 800);

    // Initialize all features
    initNavigation();
    initThemeToggle();
    initCursorGlow();
    initScrollEffects();
    initFilterButtons();
    initParticles();
    initContactForm();
    fetchData();
}

// ========================================
// PARTICLES BACKGROUND
// ========================================
function initParticles() {
    const container = document.getElementById('particlesContainer');
    if (!container) return;

    const colors = ['#6C63FF', '#FF6584', '#00d4ff', '#f39c12', '#2ecc71'];

    for (let i = 0; i < 50; i++) {
        const particle = document.createElement('div');
        particle.className = 'particle';
        const size = Math.random() * 4 + 2;
        particle.style.width = size + 'px';
        particle.style.height = size + 'px';
        particle.style.left = Math.random() * 100 + '%';
        particle.style.animationDuration = (Math.random() * 20 + 10) + 's';
        particle.style.animationDelay = (Math.random() * 10) + 's';
        particle.style.background = colors[Math.floor(Math.random() * colors.length)];
        particle.style.opacity = Math.random() * 0.3 + 0.1;
        container.appendChild(particle);
    }
}

// ========================================
// NAVIGATION
// ========================================
function initNavigation() {
    const navbar = document.getElementById('navbar');
    const menuToggle = document.getElementById('menuToggle');
    const navLinks = document.getElementById('navLinks');
    const links = document.querySelectorAll('.nav-link');

    if (menuToggle) {
        menuToggle.addEventListener('click', () => {
            menuToggle.classList.toggle('active');
            navLinks.classList.toggle('open');
        });
    }

    links.forEach(link => {
        link.addEventListener('click', () => {
            if (menuToggle) {
                menuToggle.classList.remove('active');
                navLinks.classList.remove('open');
            }
        });
    });

    // Active link on scroll
    const sections = document.querySelectorAll('section[id]');

    window.addEventListener('scroll', () => {
        let current = '';
        sections.forEach(section => {
            const sectionTop = section.offsetTop - 100;
            if (window.scrollY >= sectionTop) {
                current = section.getAttribute('id');
            }
        });

        links.forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('href') === `#${current}`) {
                link.classList.add('active');
            }
        });
    });
}

// ========================================
// THEME TOGGLE
// ========================================
function initThemeToggle() {
    const toggle = document.getElementById('themeToggle');
    if (!toggle) return;

    const icon = toggle.querySelector('i');

    // Check saved theme
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
        document.documentElement.setAttribute('data-theme', 'dark');
        if (icon) icon.className = 'fas fa-sun';
    }

    toggle.addEventListener('click', () => {
        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';

        if (isDark) {
            document.documentElement.removeAttribute('data-theme');
            if (icon) icon.className = 'fas fa-moon';
            localStorage.setItem('theme', 'light');
        } else {
            document.documentElement.setAttribute('data-theme', 'dark');
            if (icon) icon.className = 'fas fa-sun';
            localStorage.setItem('theme', 'dark');
        }
    });
}

// ========================================
// CURSOR GLOW
// ========================================
function initCursorGlow() {
    const glow = document.getElementById('cursorGlow');
    if (!glow) return;

    document.addEventListener('mousemove', (e) => {
        glow.style.left = e.clientX + 'px';
        glow.style.top = e.clientY + 'px';
    });
}

// ========================================
// SCROLL EFFECTS
// ========================================
function initScrollEffects() {
    const navbar = document.getElementById('navbar');
    if (!navbar) return;

    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    });
}

// ========================================
// FILTER BUTTONS
// ========================================
function initFilterButtons() {
    const buttons = document.querySelectorAll('.filter-btn');

    buttons.forEach(btn => {
        btn.addEventListener('click', () => {
            buttons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            filterProjects(btn.dataset.filter);
        });
    });
}

function filterProjects(filter) {
    const cards = document.querySelectorAll('.project-card');

    cards.forEach(card => {
        if (filter === 'all' || card.dataset.category === filter) {
            card.style.display = 'block';
            setTimeout(() => {
                card.style.opacity = '1';
                card.style.transform = 'translateY(0)';
            }, 50);
        } else {
            card.style.opacity = '0';
            card.style.transform = 'translateY(20px)';
            setTimeout(() => {
                card.style.display = 'none';
            }, 300);
        }
    });
}

// ========================================
// CONTACT FORM
// ========================================
function initContactForm() {
    const form = document.getElementById('contactForm');
    if (!form) return;

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const btn = form.querySelector('button[type="submit"]');
        const originalText = btn.innerHTML;

        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';
        btn.disabled = true;

        setTimeout(() => {
            btn.innerHTML = '<i class="fas fa-check"></i> Sent!';
            btn.style.background = '#48bb78';
            btn.style.boxShadow = '0 4px 20px rgba(72, 187, 120, 0.3)';

            setTimeout(() => {
                btn.innerHTML = originalText;
                btn.disabled = false;
                btn.style.background = '';
                btn.style.boxShadow = '';
                form.reset();
            }, 3000);
        }, 2000);
    });
}

// ========================================
// FETCH DATA
// ========================================
async function fetchData() {
    try {
        // Fetch stats
        const statsRes = await fetch('/api/portfolio/stats');
        if (statsRes.ok) {
            const stats = await statsRes.json();
            updateStats(stats);
            updateHeroStats(stats);
            renderLanguages(stats);
            renderSkills(stats);
        }

        // Fetch repositories
        const reposRes = await fetch('/api/portfolio/repos?excludeForks=false&excludeArchived=false&sortBy=updated');
        if (reposRes.ok) {
            const repos = await reposRes.json();
            renderProjects(repos);
        }
    } catch (error) {
        console.error('Error fetching data:', error);
        renderFallbackProjects();
    }
}

// ========================================
// UPDATE STATS
// ========================================
function updateStats(stats) {
    const elements = {
        'statRepos': stats.totalRepos || 0,
        'statStars': stats.totalStars || 0,
        'statForks': stats.totalForks || 0,
        'statLanguages': Object.keys(stats.languages || {}).length || 0
    };

    Object.keys(elements).forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = elements[id];
    });
}

function updateHeroStats(stats) {
    const heroStars = document.getElementById('totalStarsHero');
    if (heroStars) {
        heroStars.textContent = `★ ${stats.totalStars || 0}`;
    }
}

// ========================================
// RENDER PROJECTS
// ========================================
function renderProjects(repos) {
    const grid = document.getElementById('projectsGrid');
    if (!grid) return;

    const getCategory = (name) => {
        const nameLower = name.toLowerCase();
        if (nameLower.includes('mobile') || nameLower.includes('react native') || nameLower.includes('app')) return 'mobile';
        if (nameLower.includes('api') || nameLower.includes('rest') || nameLower.includes('backend')) return 'backend';
        if (nameLower.includes('full') || nameLower.includes('stack')) return 'fullstack';
        return 'web';
    };

    const projectIcons = {
        'Blood Bank Management System': '🩸',
        'Crisis Management Mobile App': '🚨',
        'Accounts Portal': '📊',
        'default': '📁'
    };

    const getProjectIcon = (name) => {
        for (const [key, value] of Object.entries(projectIcons)) {
            if (name.includes(key) || key.includes(name)) {
                return value;
            }
        }
        return projectIcons.default;
    };

    const sortedRepos = [...repos].sort((a, b) => {
        if (a.isPrivate === b.isPrivate) return 0;
        return a.isPrivate ? 1 : -1;
    });

    grid.innerHTML = sortedRepos.slice(0, 6).map((repo, index) => {
        const category = getCategory(repo.name);
        const langColor = getLanguageColor(repo.language);
        const icon = getProjectIcon(repo.name);
        const isPrivate = repo.isPrivate ? '🔒 Private' : '🌐 Public';
        const badgeClass = repo.isPrivate ? 'project-badge private' : 'project-badge public';

        return `
            <div class="project-card" data-category="${category}" style="animation-delay: ${index * 0.1}s">
                <div class="project-image" style="background: ${langColor}22;">
                    <div style="font-size: 4rem; opacity: 0.6;">${icon}</div>
                    <div class="project-overlay">
                        <a href="${repo.url}" target="_blank" class="btn btn-primary">
                            <i class="fab fa-github"></i> View Code
                        </a>
                    </div>
                </div>
                <div class="project-body">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
                        <h3 class="project-title">${repo.name}</h3>
                        <span class="${badgeClass}">${isPrivate}</span>
                    </div>
                    <p class="project-description">${repo.description || 'No description available'}</p>
                    <div class="project-meta">
                        ${repo.language ? `<span><span class="lang-dot" style="background: ${langColor}"></span>${repo.language}</span>` : ''}
                        <span><i class="fas fa-star"></i> ${repo.stars}</span>
                        <span><i class="fas fa-code-branch"></i> ${repo.forks}</span>
                        <span><i class="fas fa-clock"></i> ${repo.updatedAt}</span>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// ========================================
// RENDER FALLBACK PROJECTS
// ========================================
function renderFallbackProjects() {
    const grid = document.getElementById('projectsGrid');
    if (!grid) return;

    const cvProjects = [
        {
            name: 'Blood Bank Management System',
            description: 'Full-stack web platform managing 50+ blood donor records with Django, Python, SQLAlchemy with CSRF protection and RBAC.',
            language: 'Python',
            stars: 5,
            forks: 2,
            url: 'https://github.com/Extacia21/blood-bank',
            category: 'fullstack',
            icon: '🩸'
        },
        {
            name: 'Crisis Management Mobile App',
            description: 'Cross-platform emergency reporting app with GPS tracking, push notifications and offline-first sync using React Native, Firebase.',
            language: 'JavaScript',
            stars: 8,
            forks: 3,
            url: 'https://github.com/Extacia21/crisis-app',
            category: 'mobile',
            icon: '🚨'
        },
        {
            name: 'Accounts Portal',
            description: 'Responsive Flask site with custom Python plugins for secure client invoice portal, document uploads, and technical SEO optimization.',
            language: 'Python',
            stars: 4,
            forks: 1,
            url: 'https://github.com/Extacia21/accounts-portal',
            category: 'web',
            icon: '📊'
        }
    ];

    grid.innerHTML = cvProjects.map((project, index) => {
        const langColor = getLanguageColor(project.language);

        return `
            <div class="project-card" data-category="${project.category}" style="animation-delay: ${index * 0.1}s">
                <div class="project-image" style="background: ${langColor}22; display: flex; align-items: center; justify-content: center; font-size: 4rem; opacity: 0.6;">
                    ${project.icon}
                    <div class="project-overlay">
                        <a href="${project.url}" target="_blank" class="btn btn-primary">
                            <i class="fab fa-github"></i> View Code
                        </a>
                    </div>
                </div>
                <div class="project-body">
                    <h3 class="project-title">${project.name}</h3>
                    <p class="project-description">${project.description}</p>
                    <div class="project-meta">
                        <span><span class="lang-dot" style="background: ${langColor}"></span>${project.language}</span>
                        <span><i class="fas fa-star"></i> ${project.stars}</span>
                        <span><i class="fas fa-code-branch"></i> ${project.forks}</span>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// ========================================
// RENDER LANGUAGES
// ========================================
function renderLanguages(stats) {
    const container = document.getElementById('languagesChart');
    if (!container) return;

    const topLanguages = stats.topLanguages || [];

    if (topLanguages.length === 0) {
        container.innerHTML = `
            <div class="chart-placeholder">
                <i class="fas fa-chart-pie"></i>
                <span>No language data available</span>
            </div>
        `;
        return;
    }

    const maxCount = topLanguages[0]?.count || 1;

    container.innerHTML = topLanguages.slice(0, 8).map(lang => {
        const percentage = (lang.count / maxCount) * 100;
        const color = getLanguageColor(lang.name);

        return `
            <div class="language-item">
                <div class="lang-header">
                    <span class="lang-name">
                        <span style="color: ${color};">●</span>
                        ${lang.name}
                    </span>
                    <span class="lang-percent">${lang.percentage.toFixed(1)}%</span>
                </div>
                <div class="language-bar-container">
                    <div class="language-bar" style="width: 0%; background: ${color};" data-width="${percentage}"></div>
                </div>
            </div>
        `;
    }).join('');

    // Animate bars
    setTimeout(() => {
        document.querySelectorAll('.language-bar').forEach(bar => {
            const width = bar.dataset.width;
            bar.style.width = width + '%';
        });
    }, 300);
}

// ========================================
// RENDER SKILLS
// ========================================
function renderSkills(stats) {
    const container = document.getElementById('skillsGrid');
    if (!container) return;

    const languages = Object.keys(stats.languages || {});

    // Combine with design skills
    const designSkills = ['Photoshop', 'Illustrator', 'Figma', 'Canva', 'InDesign'];
    const allSkills = [...languages, ...designSkills];
    const uniqueSkills = [...new Set(allSkills)];
    const topSkills = uniqueSkills.slice(0, 16);

    const skillIcons = {
        'Kotlin': '🟣', 'Java': '☕', 'JavaScript': '🟡',
        'TypeScript': '🔵', 'Python': '🐍', 'HTML': '🌐',
        'CSS': '🎨', 'Go': '🐹', 'Rust': '🦀',
        'Django': '🎯', 'React': '⚛️', 'Flask': '🌶️',
        'Node.js': '🟢', 'SQLAlchemy': '🗄️', 'Firebase': '🔥',
        'AWS': '☁️', 'Linux': '🐧', 'cPanel': '🖥️',
        'MySQL': '🐬', 'PostgreSQL': '🐘', 'Photoshop': '🎨',
        'Illustrator': '✒️', 'Figma': '📐', 'Canva': '🎨',
        'InDesign': '📖'
    };

    container.innerHTML = topSkills.map(skill => {
        const icon = skillIcons[skill] || '📦';
        const level = Math.min(Math.floor(Math.random() * 30 + 70), 95);

        return `
            <div class="skill-item">
                <span class="skill-icon">${icon}</span>
                <span class="skill-name">${skill}</span>
                <div class="skill-level">
                    <div class="skill-level-bar" style="width: 0%;" data-level="${level}"></div>
                </div>
            </div>
        `;
    }).join('');

    // Animate skill bars
    setTimeout(() => {
        document.querySelectorAll('.skill-level-bar').forEach(bar => {
            const level = bar.dataset.level;
            bar.style.width = level + '%';
        });
    }, 500);
}

// ========================================
// LANGUAGE COLORS
// ========================================
function getLanguageColor(language) {
    const colors = {
        'Kotlin': '#A97BFF',
        'Java': '#B07219',
        'JavaScript': '#F1E05A',
        'TypeScript': '#3178C6',
        'Python': '#3572A5',
        'HTML': '#E34C26',
        'CSS': '#563D7C',
        'Go': '#00ADD8',
        'Rust': '#DEA584',
        'Swift': '#FFAC45',
        'PHP': '#4F5D95',
        'Ruby': '#701516',
        'C++': '#F34B7D',
        'C#': '#178600',
        'Vue': '#41B883',
        'React': '#61DAFB',
        'Angular': '#DD0031',
        'Docker': '#2496ED',
        'Kubernetes': '#326CE5',
        'AWS': '#FF9900',
        'Spring': '#6DB33F',
        'Django': '#092E20',
        'Flask': '#000000',
        'Node.js': '#339933',
        'SQLAlchemy': '#CA4242',
        'Firebase': '#FFCA28'
    };
    return colors[language] || '#6c757d';
}

// ========================================
// SMOOTH SCROLL
// ========================================
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            e.preventDefault();
            target.scrollIntoView({ behavior: 'smooth' });
        }
    });
});
