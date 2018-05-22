import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui'
import VueRouter from 'vue-router'
import About from './components/About'
import Registry from './components/Registry'
import './assets/style/element-variables.sass'
import VueMask from 'v-mask'

Vue.config.productionTip = false;
Vue.use(ElementUI);
Vue.use(VueRouter);
Vue.use(VueMask);


const routes = [
  { path: '', component: About },
  { path: '/about', component: About },
  { path: '/registry', component: Registry }
]

const router = new VueRouter({
  mode: 'history',
  routes
})

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
