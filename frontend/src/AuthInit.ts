import * as firebase from "firebase";

export function initFirebase() {
  if (firebase.apps.length === 0) {
    const firebaseConfig = {
      apiKey: "AIzaSyASK-NImbNMQ6AirjzciKb-W0cPB41_Duc",
      authDomain: "blacklist-ff68d.firebaseapp.com",
      databaseURL: "https://blacklist-ff68d.firebaseio.com",
      projectId: "blacklist-ff68d",
      storageBucket: "blacklist-ff68d.appspot.com",
      messagingSenderId: "540285171734",
      appId: "1:540285171734:web:a7481335bc79a9c2095d37",
      measurementId: "G-15KQSBGJD6"
    };
    firebase.initializeApp(firebaseConfig);
  }
}
