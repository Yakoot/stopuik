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

<script>
  import Header from "./components/Header";
  import Menu from "./components/Menu";
  import ReportModal from "./components/ReportModal";

  const regions = [
    "Санкт-Петербург",
    "Ленинградская область",
    "Псковская область",
    "Мурманская область",
  ];

  export default {
    components: {
      Header,
      Menu,
      ReportModal
    },
    data() {
      return {
        reportVisible: false,
        regions: regions,
        currentRegion: regions[0]
      };
    },
    props: [],
    computed: {},
    methods: {
      openReport() {
        this.reportVisible = true
      },
      changeRegion(region) {
        this.currentRegion = region
      }
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
