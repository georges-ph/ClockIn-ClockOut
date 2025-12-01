# ClockIn ClockOut

A lightweight, budget-friendly Android application to **Clock In** when you arrive to work and **Clock Out** when you leave.

Designed for small companies — no hardware or internet required.

## How it started

The idea came to me while watching a movie. In one scene, an employee was leaving work and had to scan his card at the company gate to clock out. It made me think: what about small companies? They usually don't have the budget to invest in such system. A dedicated hardware like card readers or access gates. So why not build a simple alternative but as a mobile app? A lightweight and affordable solution that handles employee clock-ins and clock-outs using nothing more than a phone.

## How to use

The app can be split into two roles: employee and security guard. The security guard will take care of scanning employees QR codes at the entrance.

### Guard setup (scanner)

A CSV file must first be imported from the `Profiles` screen located in the top menu. It should contain 3 columns: ID, Name and Department as such:
```csv
id,name,department
80811897,Victoria Bennett,Customer Support
33660096,Grace Stevens,Engineering
13495457,Isaac Jennings,IT
53210291,Lucas Parker,Legal
```

### Employee first-time setup

When an employee arrives, they have to scan their relevant QR code from the guard's `Profiles` screen. This will save the profile information to the employee's device.

### Daily clocking

The employee has just to tap the **In** or **Out** button which the guard has to scan.

### Attendance

The employees attendance can be viewed by tapping the `Attendance` button in the top menu. It will show when each employee has clocked in or out. Data can be exported as CSV to create Excel reports and more.