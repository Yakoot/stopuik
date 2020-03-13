import Vue from 'vue'
import Auth from './Auth.vue';
import ElementUI from 'element-ui'
import './assets/style/element-variables.scss'
import * as firebase from "firebase/app";
// Add the Firebase products that you want to use
import "firebase/auth";
import * as firebaseui from "firebaseui";

var firebaseConfig = {
  apiKey: "AIzaSyASK-NImbNMQ6AirjzciKb-W0cPB41_Duc",
  authDomain: "blacklist-ff68d.firebaseapp.com",
  databaseURL: "https://blacklist-ff68d.firebaseio.com",
  projectId: "blacklist-ff68d",
  storageBucket: "blacklist-ff68d.appspot.com",
  messagingSenderId: "540285171734",
  appId: "1:540285171734:web:a7481335bc79a9c2095d37",
  measurementId: "G-15KQSBGJD6"
};
// Initialize Firebase
firebase.initializeApp(firebaseConfig);
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
  signInSuccessUrl: '/',
  signInOptions: [
    // Leave the lines as is for the providers you want to offer your users.
    firebase.auth.EmailAuthProvider.PROVIDER_ID,
  ],
  // Terms of service url.
  tosUrl: '/about',
  // Privacy policy url.
  privacyPolicyUrl: '/about'
};
var ui = new firebaseui.auth.AuthUI(firebase.auth());
ui.start('#app', config);
//
// Vue.config.productionTip = false;
// Vue.use(ElementUI);
//
// new Vue({
//   render: h => h(Auth)
// }).$mount('#app');
