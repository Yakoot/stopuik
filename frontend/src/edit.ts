import Vue from 'vue'
import ElementUI from 'element-ui'
import ReportAdmin from "./components/ReportAdmin.vue";
import './assets/style/element-variables.scss'

Vue.config.productionTip = false;
Vue.use(ElementUI);

new Vue({
  render: h => h(ReportAdmin)
}).$mount('#app')
