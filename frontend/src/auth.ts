import './assets/style/element-variables.scss'
import * as firebase from "firebase/app";
import "firebase/auth";
import * as firebaseui from "firebaseui";
import {initFirebase} from "@/AuthInit";

initFirebase();

function getRedirectUrl(): string {
  const qd: {[key: string]: Array<string>} = {};
  if (window.location.search) {
    window.location.search.substr(1)
        .split("&")
        .forEach(item => {
          const s = item.split("=");
          const k = s[0];
          const v = s[1] ? decodeURIComponent(s[1]) : "";
          if (!qd[k]) {
            qd[k] = []
          }
          qd[k].push(v);
        });
  }
  const v = qd["redirect"];
  return v ? v[0] : "/";
}

const config = {
  callbacks: {
    signInSuccessWithAuthResult: function(authResult: any, redirectUrl: any) {
      // User successfully signed in.
      // Return type determines whether we continue the redirect automatically
      // or whether we leave that to developer to handle.
      return true;
    },
    uiShown: function() {
      // The widget is rendered.
      // Hide the loader.
      //document.getElementById('loader').style.display = 'none';
    }
  },
  // Will use popup for IDP Providers sign-in flow instead of the default, redirect.
  signInFlow: 'popup',
  signInSuccessUrl: getRedirectUrl(),
  signInOptions: [
    // Leave the lines as is for the providers you want to offer your users.
    firebase.auth.EmailAuthProvider.PROVIDER_ID,
  ],
  // Terms of service url.
  tosUrl: '/about',
  // Privacy policy url.
  privacyPolicyUrl: '/about'
};
const ui = new firebaseui.auth.AuthUI(firebase.auth());
ui.start('#app', config);
