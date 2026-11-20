# Changelog

All notable changes to this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [3.0] - 2026-11-20

Rewritten in Kotlin and Jetpack Compose with a new look. Existing data on the
gate phone and saved badges on employee phones carry over.

### Added
- The gate phone picks in or out by itself, so employees show one badge
- Anyone left out of the last import is refused at the gate
- Record a clock-out by hand, and delete a wrong record
- Share a badge as an image, for printing
- Date and time columns in the attendance export
- Records made after the phone's clock was changed are marked, in the app
  and in the export
- Dark mode

### Changed
- New design and app icon
- CSV import accepts Excel files, any column order and semicolons, and skips
  bad lines instead of failing
- IDs keep leading zeros

### Fixed
- The attendance screen crashed after an import that left out someone who
  had records
- One missed clock-out blocked that person's next clock-in
- Badges shown in dark mode could not be scanned
- Some apps could not open the shared CSV file

## [2.0] - 2025-12-01

Rewrite of the original app.

### Added
- Guard and employee roles: the guard imports profiles from CSV, employees
  save theirs by scanning a QR code
- Profiles screen with search
- Attendance screen with CSV export

## [1.1] - 2022-11-22

Initial release.

[3.0]: https://github.com/georges-ph/ClockIn-ClockOut/releases/tag/v3.0
[2.0]: https://github.com/georges-ph/ClockIn-ClockOut/releases/tag/v2.0
[1.1]: https://github.com/georges-ph/ClockIn-ClockOut/releases/tag/v1.1
