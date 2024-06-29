import tkinter as tk
from tkinter import messagebox
import subprocess

def fetch_movie_details(movie_id):
    # Call your Java method using subprocess
    result = subprocess.run(['java', 'MovieService', str(movie_id)], capture_output=True, text=True)
    return result.stdout

def on_search():
    movie_id = entry_movie_id.get()
    details = fetch_movie_details(movie_id)
    messagebox.showinfo("Movie Details", details)

# Create the main window
root = tk.Tk()
root.title("Movie Database")

# Create a label and entry for movie ID
label_movie_id = tk.Label(root, text="Movie ID:")
label_movie_id.pack()
entry_movie_id = tk.Entry(root)
entry_movie_id.pack()

# Create a search button
button_search = tk.Button(root, text="Search", command=on_search)
button_search.pack()

# Run the application
root.mainloop()
