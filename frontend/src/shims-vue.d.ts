declare module '*.vue' {
  import Vue from 'vue'
  export default Vue
}
declare module 'vue-mq';
declare module 'v-mask';
declare module 'components/About';
interface Window {
  filters: any;
  allManagingUiks: {[key: string]: object};
}
