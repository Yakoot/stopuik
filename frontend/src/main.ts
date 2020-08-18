import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui'
import VueRouter from 'vue-router'
import About from './components/About.vue'
import Registry from './components/Registry.vue'
import ReportAdmin from "./components/ReportAdmin.vue";
import './assets/style/element-variables.scss'
import VueMask from 'v-mask'
import VueMq from 'vue-mq'

Vue.config.productionTip = false;
Vue.use(ElementUI);
Vue.use(VueRouter);
Vue.use(VueMask);
Vue.use(VueMq, {
  breakpoints: { // default breakpoints - customize this
    sm: 670,
    md: 1044,
    lg: 1366,
    xl: Infinity
  }
})


const routes = [
  { path: '', component: Registry },
  { path: '/about', component: About },
  { path: '/registry', component: Registry },
  { path: '/index.html', component: Registry },
  { path: '/admin', component: ReportAdmin}
]

const router = new VueRouter({
  mode: 'history',
  routes
})

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
