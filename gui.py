import tkinter as tk
from tkinter import messagebox
import subprocess

def add_movie_to_list(user_id, movie_id, list_name):
    result = subprocess.run(
        ['java', '-cp', '.:lib/*', 'MovieService', str(user_id), str(movie_id), list_name],
        capture_output=True, text=True
    )
    if result.returncode == 0:
        messagebox.showinfo("Success", result.stdout)
    else:
        messagebox.showerror("Error", result.stderr)

def on_search():
    query = search_entry.get()
    if not query:
        messagebox.showwarning("Input Error", "Please enter a movie name.")
        return

    # For simplicity, let's assume the query returns a single movie ID (replace with actual search logic)
    movie_id = 550  # Replace with actual movie ID from search results
    add_movie_to_list(1, movie_id, "watchlist")

def on_watch():
    query = search_entry.get()
    if not query:
        messagebox.showwarning("Input Error", "Please enter a movie name.")
        return

    # For simplicity, let's assume the query returns a single movie ID (replace with actual search logic)
    movie_id = 550  # Replace with actual movie ID from search results
    add_movie_to_list(1, movie_id, "watched")

root = tk.Tk()
root.title("Movie Search")

tk.Label(root, text="Search for a Movie:").grid(row=0, column=0)
search_entry = tk.Entry(root, width=50)
search_entry.grid(row=0, column=1)

search_button = tk.Button(root, text="Add to Watchlist", command=on_search)
search_button.grid(row=0, column=2)

watch_button = tk.Button(root, text="Add to Watched", command=on_watch)
watch_button.grid(row=1, column=2)

root.mainloop()
