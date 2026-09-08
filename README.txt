MoneyMate V2.6 Final Stable

Fix: startup persistence race. IndexedDB restore now completes before the first render/save, so refresh will not temporarily show zero or overwrite saved transactions. Service worker cache bumped to v2.6.0.
