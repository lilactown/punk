# Changelog

## [0.0.7] - Feb 13, 2019

### Fixes

- Fix padding around entries ID
- Fix #8: margin around body in Node.js adapter
- Fix #9: Long key names in map view don't look right
- Fix #11: Long unbroken strings display wrong in tables

## [0.0.6] - Feb 12, 2019

 - Previewing a value in the **Next** pane doesn't call `nav` on the value.
 Instead, `nav` is only called once the value moves into the **Current** pane.

## [0.0.5] - Feb 11, 2019

 - Fixed https://github.com/Lokeh/punk/issues/2 for unknown reader literals on 
 both server and client

## [0.0.4] - Feb 10, 2019

### Changed

- Moved all styles to `punk.css`

## [0.0.3] - Feb 2, 2019

### Added

- Users may now select different views for the Current pane
- Users may now navigate the history via bread crumbs on the bottom of the Current pane

### Changed
- View selection moved to controls area on bottom of panes
