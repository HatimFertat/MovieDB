import tkinter as tk
from tkinter import messagebox, ttk
import subprocess
import json

def add_movie_to_list(user_id, movie_id, list_name):
    result = subprocess.run(
        ['java', '-cp', '.:lib/*', 'MovieService', str(user_id), str(movie_id), list_name],
        capture_output=True, text=True
    )
    if result.returncode == 0:
        messagebox.showinfo("Success", result.stdout)
        update_list_view(user_id, list_name)
    else:
        messagebox.showerror("Error", result.stderr)

def fetch_movie_details(query):
    result = subprocess.run(
        ['java', '-cp', '.:lib/*', 'MovieService', query],
        capture_output=True, text=True
    )
    if result.returncode == 0:
        return json.loads(result.stdout)
    else:
        messagebox.showerror("Error", result.stderr)
        return None

def on_search():
    query = search_entry.get()
    if not query:
        messagebox.showwarning("Input Error", "Please enter a movie name.")
        return

    movie_details = fetch_movie_details(query)
    if movie_details:
        movie_id = movie_details['results'][0]['id']
        display_movie_details(movie_details['results'][0])
        add_movie_to_list(1, movie_id, "watchlist")

def on_watch():
    query = search_entry.get()
    if not query:
        messagebox.showwarning("Input Error", "Please enter a movie name.")
        return

    movie_details = fetch_movie_details(query)
    if movie_details:
        movie_id = movie_details['results'][0]['id']
        display_movie_details(movie_details['results'][0])
        add_movie_to_list(1, movie_id, "watched")

def display_movie_details(movie):
    details_text.delete(1.0, tk.END)
    details = (
        f"Title: {movie['title']}\n"
        f"Release Date: {movie['release_date']}\n"
        f"Director: {movie.get('director', 'N/A')}\n"
        f"Cast: {', '.join(movie.get('cast', []))}\n"
        f"Genres: {', '.join(genre['name'] for genre in movie['genres'])}\n"
        f"Synopsis: {movie['overview']}\n"
    )
    details_text.insert(tk.END, details)

def update_list_view(user_id, list_name):
    listbox.delete(0, tk.END)
    # Fetch the list from ScalarDB and populate the listbox (placeholder logic)
    movies = ["Movie 1", "Movie 2", "Movie 3"]  # Replace with actual fetch logic
    for movie in movies:
        listbox.insert(tk.END, movie)

root = tk.Tk()
root.title("Movie Search")

tk.Label(root, text="Search for a Movie:").grid(row=0, column=0)
search_entry = tk.Entry(root, width=50)
search_entry.grid(row=0, column=1)

search_button = tk.Button(root, text="Add to Watchlist", command=on_search)
search_button.grid(row=0, column=2)

watch_button = tk.Button(root, text="Add to Watched", command=on_watch)
watch_button.grid(row=1, column=2)

details_text = tk.Text(root, width=80, height=10)
details_text.grid(row=2, column=0, columnspan=3)

tk.Label(root, text="Movies in List:").grid(row=3, column=0, sticky=tk.W)
listbox = tk.Listbox(root, width=80, height=10)
listbox.grid(row=4, column=0, columnspan=3)

root.mainloop()
