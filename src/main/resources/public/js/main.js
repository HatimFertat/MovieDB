document.addEventListener('DOMContentLoaded', () => {
    const currentPath = window.location.pathname;

    if (currentPath === '/') {
        initLoginPage();
    } else if (currentPath === '/register') {
        initRegisterPage();
    } else if (currentPath === '/profile') {
        initProfilePage();
    } else if (currentPath === '/watchlist') {
        initWatchlistPage();
    } else if (currentPath === '/watched') {
        initWatchedPage();
    } else if (currentPath === '/search') {
        initSearchPage();
    }
});

//#region Authentication Functions
async function handleLogin(event) {
    event.preventDefault();
    const userId = document.querySelector('#userId').value;
    const password = document.querySelector('#password').value;

    console.log(`Logging in with userId: ${userId}`);

    try {
        const response = await makeApiRequest('/api/login', 'POST', { userId, password });
        if (response.ok) {
            // Store the user ID in localStorage
            localStorage.setItem('userId', userId);
            window.location.href = '/profile';
        } else {
            showError('Invalid user ID or password');
        }
    } catch (error) {
        showError('Failed to login');
    }
}

function signOut() {
    // Clear any user-related data (e.g., userId from localStorage or sessionStorage)
    localStorage.removeItem('userId');
    // Redirect to the login page
    window.location.href = '/';
}


async function handleRegister(event) {
    event.preventDefault();
    const userId = document.querySelector('#userId').value;
    const email = document.querySelector('#email').value;
    const password = document.querySelector('#password').value;

    console.log(`Registering with userId: ${userId}, email: ${email}`);

    try {
        const response = await makeApiRequest('/api/register', 'POST', { userId, email, password });
        if (response.ok) {
            window.location.href = '/';
        } else {
            showError('User already exists or invalid input');
        }
    } catch (error) {
        showError('Failed to register');
    }
}
//#endregion

//#region Movie List Management Functions
async function loadWatchlist() {
    const userId = getUserId();
    if (!userId) {
        showError('User not logged in');
        return;
    }
    try {
        const response = await makeApiRequest(`/api/listWatchlist?userId=${userId}`, 'GET');
        const movies = await response.json();
        displayMovies(movies, 'watchlist');
    } catch (error) {
        showError('Failed to load watchlist');
    }
}

async function loadWatchedList() {
    const userId = getUserId();
    if (!userId) {
        showError('User not logged in');
        return;
    }
    try {
        const response = await makeApiRequest(`/api/listWatched?userId=${userId}`, 'GET');
        const movies = await response.json();
        displayMovies(movies, 'watched');
    } catch (error) {
        showError('Failed to load watched list');
    }
}

function displayMovies(movies, elementId) {
    const container = document.querySelector(`#${elementId}`);
    clearElement(elementId);
    movies.forEach(movie => {
        const movieElement = createMovieElement(movie, elementId); //elementId is the same as listName
        container.appendChild(movieElement);
    });
}

async function moveToWatched(movieId) {
    const userId = getUserId();
    try {
        const response = await makeApiRequest('/api/moveToWatched', 'POST', {
            userId: userId,
            movieId: String(movieId),
            listName: "listName"
        });
        if (response.ok) {
            showSuccess('Movie moved to watched list');
            loadWatchlist(); // Reload the watchlist after moving the movie
        } else {
            throw new Error('Failed to move movie to watched list');
        }
    } catch (error) {
        showError('Failed to move movie to watched list');
        console.error('Error moving movie to watched list:', error);
    }
}

async function addToList(movieId, listName) {
    const userId = getUserId();
    if (!userId) {
        showError('User not logged in');
        return;
    }
    try {
        const response = await makeApiRequest('/api/addMovie', 'POST', {
            userId: userId,
            movieId: String(movieId),
            listName: listName
        });
        console.log(`Response status: ${response.status}`); // Log the response status
        if (response.ok) {
            if (listName === 'watchlist') {
                showSuccess('Movie added to watchlist');
            } else if (listName === 'watched') {
                showSuccess('Movie added to watched');
            }
        } else {
            const errorText = await response.text(); // Get error text from the response
            showError(`Failed to add movie to ${listName}: ${errorText}`);
            console.log(`Error text: ${errorText}`); // Log the error text
        }
    } catch (error) {
        showError(`Failed to add movie to ${listName}`);
        console.error(error); // Log the error for debugging
    }
}


async function deleteMovie(movieId, listName) {
    const userId = getUserId();
    if (!userId) {
        showError('User not logged in');
        return;
    }
    try {
        const response = await makeApiRequest('/api/deleteMovie', 'DELETE', {
            userId: userId,
            movieId: movieId,
            listName: listName
        });
        if (response.ok) {
            showSuccess('Deleted movie successfully');
        } else {
            throw new Error('Failed to delete movie');
        }
    } catch (error) {
        showError('Failed to delete movie');
        console.error('Error deleting movie:', error);
    }
}

//#endregion

//#region Search Functions
async function handleSearch(event) {
    event.preventDefault();
    const query = document.querySelector('#query').value;
    const searchType = document.querySelector('.search-type-btn.active').id;

    console.log(`Searching for: ${query}, Search Type: ${searchType}`);

    try {
        let response;
        if (searchType === 'local-search') {
            response = await makeApiRequest(`/api/searchMovies?query=${query}`, 'GET');
        } else if (searchType === 'tmdb-search') {
            response = await makeApiRequest(`/api/searchTMDBMovies?query=${query}`, 'GET');
        }
        const movies = await response.json();
        displaySearchResults(movies);
    } catch (error) {
        console.error("Error: ", error); // Add logging
        showError('Failed to search movies');
    }
}


function displaySearchResults(movies) {
    const container = document.querySelector('#search-results');
    clearElement('search-results');
    movies.forEach(movie => {
        const movieElement = createMovieElement(movie, "search");
        container.appendChild(movieElement);
    });
}
//#endregion

//#region Utility Functions
function createMovieElement(movie, listName) {
    const movieDiv = document.createElement('div');
    movieDiv.classList.add('movie');

    const title = document.createElement('div');
    title.classList.add('movie-title');
    title.textContent = movie.title;

    const details = document.createElement('div');
    details.classList.add('movie-details');

    details.textContent = `Release Date: ${movie.release_date}, Genres: ${convertGenreIdsToNames(movie.genre_ids)}`;

    const actions = document.createElement('div');
    actions.classList.add('movie-actions');

    let showDeleteButton = false;
    let showMoveToWatchedButton = false;
    let showAddButtons = false;

    if (listName === 'watched') {
        showDeleteButton = true;
    } else if (listName === 'watchlist') {
        showDeleteButton = true;
        showMoveToWatchedButton = true;
    } else if (listName === 'search') {
        showAddButtons = true;
    }

    if (showDeleteButton) {
        const deleteButton = document.createElement('button');
        deleteButton.textContent = 'Delete';
        deleteButton.addEventListener('click', () => {
            deleteMovie(movie.id, listName);
        });
        actions.appendChild(deleteButton);
    }

    if (showMoveToWatchedButton) {
        const moveToWatchedButton = document.createElement('button');
        moveToWatchedButton.textContent = 'Move to Watched';
        moveToWatchedButton.addEventListener('click', () => {
            moveToWatched(movie.id);
        });
        actions.appendChild(moveToWatchedButton);
    }

    if (showAddButtons) {
        const addToWatchlistButton = document.createElement('button');
        addToWatchlistButton.textContent = 'Add to Watchlist';
        addToWatchlistButton.addEventListener('click', () => {
            addToList(movie.id, "watchlist");
        });
        actions.appendChild(addToWatchlistButton);

        const addToWatchedButton = document.createElement('button');
        addToWatchedButton.textContent = 'Add to Watched';
        addToWatchedButton.addEventListener('click', () => {
            addToList(movie.id, "watched");
        });
        actions.appendChild(addToWatchedButton);
    }

    movieDiv.appendChild(title);
    movieDiv.appendChild(details);
    movieDiv.appendChild(actions);

    return movieDiv;
}

function showSuccess(message) {
    const successElement = document.createElement('div');
    successElement.className = 'success-message';
    successElement.textContent = message;
    document.body.appendChild(successElement);
    setTimeout(() => {
        document.body.removeChild(successElement);
    }, 3000);
}

function showError(message) {
    const errorElement = document.createElement('div');
    errorElement.className = 'error-message';
    errorElement.textContent = message;
    document.body.appendChild(errorElement);
    setTimeout(() => {
        document.body.removeChild(errorElement);
    }, 3000);
}

function clearElement(elementId) {
    const element = document.querySelector(`#${elementId}`);
    while (element.firstChild) {
        element.removeChild(element.firstChild);
    }
}

async function makeApiRequest(url, method, body = null) {
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json'
        }
    };
    if (body) {
        options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);
    return response;
}

function getUserId() {
    return localStorage.getItem('userId');
}
//#endregion

//#region Page-specific Initialization
function initLoginPage() {
    document.querySelector('#login-form').addEventListener('submit', handleLogin);
}

function initRegisterPage() {
    document.querySelector('#register-form').addEventListener('submit', handleRegister);
}

async function initProfilePage() {
    const userId = getUserId();
    if (!userId) {
        showError('User not logged in');
        return;
    }
    try {
        const response = await makeApiRequest(`/api/userProfile?userId=${userId}`, 'GET');
        const profile = await response.json();
        const profileInfo = document.querySelector('#profile-info');
        profileInfo.textContent = `User ID: ${profile.userId}, Email: ${profile.email}`;
    } catch (error) {
        showError('Failed to load profile information');
    }
}
//#endregion

//#region Other init functions


function initWatchlistPage() {
    loadWatchlist();
}

function initWatchedPage() {
    loadWatchedList();
}

function initSearchPage() {
    document.querySelector('#search-form').addEventListener('submit', handleSearch);

    const localSearchBtn = document.querySelector('#local-search');
    const tmdbSearchBtn = document.querySelector('#tmdb-search');

    localSearchBtn.addEventListener('click', () => {
        localSearchBtn.classList.add('active');
        tmdbSearchBtn.classList.remove('active');
    });

    tmdbSearchBtn.addEventListener('click', () => {
        tmdbSearchBtn.classList.add('active');
        localSearchBtn.classList.remove('active');
    });
}

const GENRE_MAP = {
    28: "Action",
    12: "Adventure",
    16: "Animation",
    35: "Comedy",
    80: "Crime",
    99: "Documentary",
    18: "Drama",
    10751: "Family",
    14: "Fantasy",
    36: "History",
    27: "Horror",
    10402: "Music",
    9648: "Mystery",
    10749: "Romance",
    878: "Science Fiction",
    10770: "TV Movie",
    53: "Thriller",
    10752: "War",
    37: "Western"
};

function convertGenreIdsToNames(genreIds) {
    if (!genreIds) {
        return "Unknown";
    }
    
    let genreNames;
    if (typeof genreIds === 'string') {
        genreNames = genreIds.split(',').map(id => GENRE_MAP[id.trim()] || "Unknown");
    } else if (Array.isArray(genreIds)) {
        genreNames = genreIds.map(id => GENRE_MAP[id] || "Unknown");
    } else {
        genreNames = ["Unknown"];
    }

    return genreNames.join(", ");
}
//#endregion

// Main Initialization
function init() {
    const currentPath = window.location.pathname;

    if (currentPath === '/') {
        initLoginPage();
    } else if (currentPath === '/register') {
        initRegisterPage();
    } else if (currentPath === '/profile') {
        initProfilePage();
    } else if (currentPath === '/watchlist') {
        initWatchlistPage();
    } else if (currentPath === '/watched') {
        initWatchedPage();
    } else if (currentPath === '/search') {
        initSearchPage();
    }
}

document.addEventListener('DOMContentLoaded', init);
