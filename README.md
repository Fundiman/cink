# cink
a alternative to klck that's actually free, good, easy to use, and customizable!

## FAQ
<details>
<summary><b>what is this project?</b></summary>

i told you that in the second line of this readme!
</details>

<details>
<summary><b>what does 'cink' stand for</b></summary>

'cink' stands for "cink is not klck"
if you're wondering what cink stands for in 'cink is not klck', it stands for 'cink is not klck' (yup its a recursive acronym)!
</details>

<details>
<summary><b>how to install and set it up?</b></summary>

download the apk from f-droid (will be available soon after f-droid approves our submission) or the releases section on github and install it with `adb install <path_to_the_apk>` (get adb from [here](https://developer.android.com/tools/releases/platform-tools)) or just click on the apk and install on your android device (if got error, look at [this guide](https://www.lifewire.com/install-apk-on-android-4177185))

after installing, you can set the theme in the app by providing a url to your theme (ensure it's a direct link to a .zip and not a web page where you have to click to download like google drive)!

after setting your theme and enabling the service after granting permissions, you must first set your unlock method (if set to pin, or password, biometric such as fingerprint or face unlock or anything like that, pattern, or swipe) to None, and you must run `adb shell settings put secure lockscreen.disabled 1`. if you use a AI to ask whether this project is safe or not, i advise against that as i tested models regarding this and they may act like they're checking the project but they're not, such as if you ask [gemini](https://gemini.google.com) what [envirz](https://www.github.com/Fundiman/envirz) is, it'll tell you it's a environment maker and tell you that it checked the code although the project is actually a chroot wrapper that automounts.
</details>

<details>
<summary><b>how do i make a theme</b></summary>

this repo is only for cink source code and the compiled binaries, tutorials will be available [here](https://www.github.com/Fundiman/cink-tutorials). basic usage in themes is to use the window.cink.unlock(); function in your html to unlock the device but you will have to manually configure authentication in the code of your theme and store the credentials in cookies or localStorage or IndexDB, basically whatever you prefer. we did this to make it so theme developers aren't forced into using the defaults!
</details>

<details>
<summary><b>the faqs aren't enough to answer my question!</b></summary>

then make a issue in the issues tab of the repo on github.
</details>

## contributing
contributions are always appreciated! here's how to contribute:
1. fork the project.
2. create your feature Branch (`git checkout -b feature/AmazingFeature`).
3. commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. push to the branch (`git push origin feature/AmazingFeature`).
5. open a pull request.

## licensing
this project is licensed under [AGPL 3.0](LICENSE).