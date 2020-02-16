<template>
  <div id="app">
    <div>
      <Header :regions="regions"
              :currentRegion="currentRegion"
              @change-region="changeRegion"></Header>
      <Menu @openReport="openReport"/>
    </div>
    <div>
      <router-view @openReport="openReport"></router-view>
    </div>
    <ReportModal :visible.sync="reportVisible" @closeReport="reportVisible = false"/>
  </div>
</template>

<script lang="ts">
  import {Component, Vue} from 'vue-property-decorator';
  import Header from "./components/Header.vue";
  import Menu from "./components/Menu.vue";
  import ReportModal from "./components/ReportModal.vue";

  const regions = [
    "Санкт-Петербург",
    "Ленинградская область",
    "Псковская область",
    "Мурманская область",
  ];

  @Component({
    components: {
      Header,
      Menu,
      ReportModal
    },
  })
  export default class App extends Vue {
    private reportVisible = false;
    private regions = regions;
    private currentRegion: string = regions[0];

    openReport() {
        this.reportVisible = true
    }
    changeRegion(region: string) {
        this.currentRegion = region;
    }
  };
</script>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  /* margin-top: 60px; */
}
</style>
