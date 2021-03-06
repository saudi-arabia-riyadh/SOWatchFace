# SO Watch Face

Simple watch face with Stack Overflow and Stack Exchange design

![Watch face](https://i.stack.imgur.com/WWPoE.jpg)

Made for the https://meta.stackexchange.com/a/319326/133343 competition
for fun and is NOT owned by, operated by, endorsed by, or in any way part of Stack Exchange Inc.

## Download

- Play store link: https://play.google.com/store/apps/details?id=hu.sztupy.sowatchface
- Releases: https://github.com/sztupy/SOWatchFace/releases
- Demo video (v5.0): https://youtu.be/NVwtpdLeB2g
- StackApps link: https://stackapps.com/q/8176/1265

## Features

- Two watch faces, a simple one, and another that looks like the SWAG you can win.
- Supports two complications on the watch face.
- Shows the logo of a selected Stack Exchange site as the background
  - Obtains the logo on the fly
- Includes a complication that can show a specific user's reputation on any Stack Exchange site
  - Displays the next reputation milestone as the range data.
  - Works on other watch faces that support complications as well.
  - Shows Jon Skeet's Stack Overflow reputation by default
- Includes a complication that just shows the logo of a particular Stack Exchange site.
- Displays the UTC offset so you know when the Stack Overflow counters reset.
- Water Resistant to 6-8 weeks

## Usage

- Select the "SO Watch Face" in the watch face list to select it. Press the small cog to open up the settings.
- Options available:

  - Complications: You can set the left and right complication
  - Unread Notifications: You can set whether you would like to see the unread notification dot or not
  - Display UTC Notch: You can enable/disable displaying when UTC midnight is on the watch face
  - SWAG Watch Face: You can switch between the simpler display featuring a larger logo, or the more complex one that loogs like the SWAG you could win
  - SE Site: Allows you to change which Stack Exchange site you wish to look at
  - SE ID: Allows you to set which Stack Exchange Network User ID you wish the reputation complication to use. Pleae note: you have to set the site-wide network ID here, not the site specific ID.
  - Site ID: This will show you the ID the reputation complications will use. This is a read-only field.

- Also has two complications you can use in any watch face. They can be configured in the watch face configuration

## Potential improvements

- The list of Stack Exchange sites used is static and is from 12/12/2018. Any change
  after that in the list of available Stack Exchange sites is not reflected in the app.
  The list should be dynamic.
- The watch face and all complications use the same global setting for the selected site
  and selected user, meaning you can't have different settings for each of them. There
  should be separate settings for each complication and the watch face.
- Logos once downloaded are never updated - you need to clear the application's Data files
  to purge them. There should be a mechanism to clear them out occasionally.
- Color Scheme is aligned with Stack Overflow's site and not the other pages. The scheme should
  also dynamically update based on the chosen site.  

## License

Code is Licensed under the GPLv3

The app downloads logos and images from the Stack Exchange network to display them along with the data. 
Those logos are Copyrighted by Stack Exchange Inc.

The code was made for fun and is NOT owned by, operated by, endorsed by, or in any way part of Stack Exchange Inc.
