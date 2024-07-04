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

async function loadProfileInfo() {
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
async function loadList(listName) {
    const userId = getUserId();
    if (!userId) {
        showError('User not logged in');
        return;
    }
    try {
        const response = await makeApiRequest(`/api/list${listName}?userId=${userId}`, 'GET');
        const movies = await response.json();
        displayMovies(movies, listName);
    } catch (error) {
        showError('Failed to load list' + listName);
    }
}

async function loadFriendWatchedList(friendId) {
    const userId = getUserId();
    if (!userId) {
        showError('User not logged in');
        return;
    }
    try {
        const response = await makeApiRequest(`/api/listwatched?userId=${friendId}`, 'GET');
        const movies = await response.json();
        displayMovies(movies, "watched");
    } catch (error) {
        showError(`Failed to load ${friendId}'s watched list`);
    }
}


function displayMovies(movies, elementId) {
    const container = document.querySelector(`#${elementId}`);
    clearElement(container);
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
            showSuccess('Movie moved to Watched list');
            loadList("watchlist"); // Reload the watchlist after moving the movie
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
            showSuccess('Movie added to ' + listName);
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
            removeMovieElement(movieId, listName);
        } else {
            throw new Error('Failed to delete movie');
        }
    } catch (error) {
        showError('Failed to delete movie');
        console.error('Error deleting movie:', error);
    }
}

function removeMovieElement(movieId, listName) {
    const movieElement = document.querySelector(`#movie-${listName}-${movieId}`);
    if (movieElement) {
        movieElement.remove();
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
    movieDiv.id = `movie-${listName}-${movie.id}`;

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

//*region Friends

async function searchUsers() {
    const query = document.querySelector('#user-search').value;
    window.location.href = `/search?type=users&query=${encodeURIComponent(query)}`;
}

async function performUserSearch(query) {
    const userId = getUserId();
    try {
        const response = await makeApiRequest(`/api/searchUsers?query=${encodeURIComponent(query)}`, 'GET');
        const users = await response.json();
        
        // Fetch pending requests to check status
        const requestsResponse = await makeApiRequest(`/api/listFriendRequests?userId=${userId}`, 'GET');
        const pendingRequests = await requestsResponse.json();

        // Add request status to user search results
        users.forEach(user => {
            const request = pendingRequests.find(req => 
                (req.requester_id === userId && req.requestee_id === user.userId) || 
                (req.requestee_id === userId && req.requester_id === user.userId)
            );
            if (request) {
                user.requestStatus = request.status;
                user.requester_id = request.requester_id;
                user.requestee_id = request.requestee_id;
            }
        });

        displayUserSearchResults(users);
    } catch (error) {
        showError('Failed to search users');
    }
}


function displayUserSearchResults(users) {
    const container = document.querySelector('#search-results');
    clearElement(container);  // Pass the element directly
    users.forEach(user => {
        const userElement = createUserElement(user, 'search');
        container.appendChild(userElement);
    });
}

function createUserElement(user, type) {
    const userDiv = document.createElement('div');
    userDiv.classList.add('user');

    const userId = document.createElement('div');
    userId.classList.add('user-id');
    userId.textContent = user.userId || user.requester_id || user.requestee_id || user.friendId;

    const actions = document.createElement('div');
    actions.classList.add('user-actions');

    if (type === 'search') {
        // Check if a request already exists
        if (user.requestStatus) {
            if (user.requestStatus === 'pending') {
                if (user.requester_id === getUserId()) {
                    // Request sent by the current user
                    const removeButton = document.createElement('button');
                    removeButton.classList.add('button-red');
                    removeButton.textContent = 'Remove Request';
                    removeButton.onclick = () => removeFriendRequest(user.requestee_id);
                    actions.appendChild(removeButton);
                } else {
                    // Request received by the current user
                    const acceptButton = document.createElement('button');
                    acceptButton.textContent = 'Accept';
                    acceptButton.classList.add('button-accept');
                    acceptButton.onclick = () => acceptFriendRequest(user.requester_id);
                    const declineButton = document.createElement('button');
                    declineButton.classList.add('button-red');
                    declineButton.textContent = 'Decline';
                    declineButton.onclick = () => declineFriendRequest(user.requester_id);
                    actions.appendChild(acceptButton);
                    actions.appendChild(declineButton);
                }
            }
        } else {
            // No existing request
            const actionButton = document.createElement('button');
            actionButton.textContent = 'Send Request';
            actionButton.onclick = () => sendFriendRequest(getUserId(), user.userId);
            actions.appendChild(actionButton);
        }
    } else if (type === 'request') {
        if (user.requester_id === getUserId()) {
            // Request sent by the current user
            const removeButton = document.createElement('button');
            removeButton.classList.add('button-red');
            removeButton.textContent = 'Remove Request';
            removeButton.onclick = () => removeFriendRequest(user.requestee_id);
            actions.appendChild(removeButton);
        } else {
            // Request received by the current user
            const acceptButton = document.createElement('button');
            acceptButton.textContent = 'Accept';
            acceptButton.classList.add('button-accept');
            acceptButton.onclick = () => acceptFriendRequest(user.requester_id);
            const declineButton = document.createElement('button');
            declineButton.textContent = 'Decline';
            declineButton.classList.add('button-red');
            declineButton.onclick = () => declineFriendRequest(user.requester_id);
            actions.appendChild(acceptButton);
            actions.appendChild(declineButton);
        }
    } else if (type === 'friend') {
        console.log("create element")
        console.log(user.friendId)
        const watchedButton = document.createElement('button');
        watchedButton.classList.add('button-blue');
        watchedButton.textContent = 'See Watched List';
        watchedButton.onclick = () => window.location.href = `/watched?friendId=${user.friendId}`;
        const removeButton = document.createElement('button');
        removeButton.classList.add('button-red');
        removeButton.textContent = 'Remove';
        removeButton.onclick = () => removeFriend(user.friendId);
        actions.appendChild(watchedButton);
        actions.appendChild(removeButton);
    }

    userDiv.appendChild(userId);
    userDiv.appendChild(actions);

    return userDiv;
}

async function sendFriendRequest(requesterId, requesteeId) {
    try {
        const response = await makeApiRequest('/api/sendFriendRequest', 'POST', {
            requesterId,
            requesteeId
        });
        if (response.ok) {
            showSuccess('Friend request sent');
            updateUserSearchResult(requesteeId, 'Remove Request');
        } else {
            showError('Failed to send friend request');
        }
    } catch (error) {
        showError('Failed to send friend request');
    }
}

async function acceptFriendRequest(requesterId) {
    const requesteeId = getUserId();
    try {
        const response = await makeApiRequest('/api/acceptFriendRequest', 'POST', {
            requesterId,
            requesteeId
        });
        if (response.ok) {
            showSuccess('Friend request accepted');
            loadFriendRequests();
            loadFriends();
        } else {
            showError('Failed to accept friend request');
        }
    } catch (error) {
        showError('Failed to accept friend request');
    }
}

async function declineFriendRequest(requesterId) {
    const requesteeId = getUserId();
    try {
        const response = await makeApiRequest('/api/declineFriendRequest', 'POST', {
            requesterId,
            requesteeId
        });
        if (response.ok) {
            showSuccess('Friend request declined');
            loadFriendRequests();
            loadFriends();
        } else {
            showError('Failed to decline friend request');
        }
    } catch (error) {
        showError('Failed to decline friend request');
    }
}

async function removeFriendRequest(requesteeId) {
    const requesterId = getUserId();
    try {
        const response = await makeApiRequest('/api/removeFriendRequest', 'POST', {
            requesterId,
            requesteeId
        });
        if (response.ok) {
            showSuccess('Friend request removed');
            loadFriendRequests();
            loadFriends();
        } else {
            showError('Failed to remove friend request');
        }
    } catch (error) {
        showError('Failed to remove friend request');
    }
}

async function removeFriend(friendId) {
    const userId = getUserId();
    try {
        const response = await makeApiRequest('/api/removeFriend', 'POST', {
            userId,
            friendId
        });
        if (response.ok) {
            showSuccess('Friend request removed');
            loadFriendRequests();
            loadFriends();
        } else {
            showError('Failed to remove friend request');
        }
    } catch (error) {
        showError('Failed to remove friend request');
    }
}


function updateUserSearchResult(userId, actionText) {
    const userElements = document.querySelectorAll('.user');
    userElements.forEach(userElement => {
        const idElement = userElement.querySelector('.user-id');
        if (idElement && idElement.textContent === userId) {
            const actionButton = userElement.querySelector('button');
            if (actionButton) {
                actionButton.textContent = actionText;
            }
        }
    });
}

function clearElement(element) {
    while (element.firstChild) {
        element.removeChild(element.firstChild);
    }
}

async function loadFriendRequests() {
    const userId = getUserId();
    try {
        const response = await makeApiRequest(`/api/listFriendRequests?userId=${userId}`, 'GET');
        const requests = await response.json();
        displayFriendRequests(requests);
    } catch (error) {
        showError('Failed to load friend requests');
    }
}

async function loadFriends() {
    const userId = getUserId();
    try {
        const response = await makeApiRequest(`/api/listFriends?userId=${userId}`, 'GET');
        const friends = await response.json();
        displayFriends(friends);
    } catch (error) {
        showError('Failed to load friends');
    }
}

function displayFriendRequests(requests) {
    const container = document.querySelector('#pending-requests');
    clearElement(container);
    requests.forEach(request => {
        const requestElement = createUserElement(request, 'request');
        container.appendChild(requestElement);
    });
}

function displayFriends(friends) {
    const container = document.querySelector('#friends');
    clearElement(container);
    friends.forEach(friend => {
        const friendElement = createUserElement(friend, 'friend');
        container.appendChild(friendElement);
    });
}

//*endregion

//#region Page-specific Initialization
function initLoginPage() {
    document.querySelector('#login-form').addEventListener('submit', handleLogin);
}

function initRegisterPage() {
    document.querySelector('#register-form').addEventListener('submit', handleRegister);
}

async function initProfilePage() {
    loadProfileInfo();
    loadFriendRequests();
    loadFriends();
}
//#endregion

//#region Other init functions


function initWatchlistPage() {
    loadList("watchlist");
}

function initWatchedPage() {
    const urlParams = new URLSearchParams(window.location.search);
    const friendId = urlParams.get('friendId');
    if (friendId) {
        loadFriendWatchedList(friendId);
        document.title = `Watched Movies - ${friendId}`;
        const headerTitle = document.getElementById('watched-title');
        if (headerTitle) {
            headerTitle.textContent = `Watched Movies - ${friendId}`;
        }
    } else {
        loadList("watched");
        document.title = 'Watched Movies';
        const headerTitle = document.getElementById('watched-title');
        if (headerTitle) {
            headerTitle.textContent = 'Watched Movies';
        }
    }
}

function initSearchPage() {
    const userSearchButton = document.getElementById('user-search-btn');
    const searchForm = document.getElementById('search-form');
    const searchTitle = document.getElementById('search-title');

    userSearchButton.addEventListener('click', searchUsers);

    const urlParams = new URLSearchParams(window.location.search);
    const searchType = urlParams.get('type');
    const query = urlParams.get('query');

    if (searchType === 'users') {
        searchForm.style.display = 'none';
        searchTitle.textContent = 'Search Users';
        if (query) {
            performUserSearch(query);
        }
    } else {
        searchForm.style.display = 'block';
        searchTitle.textContent = 'Search Movies';
        document.getElementById('search-form').addEventListener('submit', handleSearch);
    }
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
