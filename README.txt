MoneyMate V2.7 Refresh-Proof

This build strengthens persistence for GitHub Pages/PWA use.
- IndexedDB is restored before the first render.
- LocalStorage is used as a fast mirror and fallback.
- A timestamp prevents an older/empty LocalStorage state from overwriting newer IndexedDB data.
- Invalid local JSON is safely ignored instead of breaking startup.
- Service worker cache is versioned to v2.7.0 and HTML uses network-first/no-store fetching.

Upload/replace these files in the GitHub Pages repository root:
index.html
manifest.json
sw.js
icon.svg
README.txt
