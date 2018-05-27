<template>
  <div class="registry"
       v-loading="loading">
    <div class="registry-filter">
      <a v-for="item in letters" :key="item" class="filter-item" :href="'#' + item">{{ item }}</a>
    </div>
    <el-collapse accordion v-model="activeName">
      <LetterBlock
          v-for="(item, key) in items"
          :key="key"
          :letter="key"
          :items="item"/>
    </el-collapse>
  </div>
</template>
<script>
  import axios from "axios";

  import RegistryFilter from './RegistryFilter'
  import LetterBlock from './LetterBlock'
  export default {
    components: {RegistryFilter, LetterBlock},
    data() {
      return {
        data: {},
        letters: [],
        loading: true,
        activeName: []
      };
    },
    computed: {
      items() {
        return this.data
      }
    },
    props: [],
    methods: {
      getData() {
        axios.get("/api.php", {
          params: {
            method: "getData",
            sheet: "Санкт-Петербург"
          }
        })
          .then(response => {
            this.data = response.data
            this.letters = Object.keys(this.data)
            this.loading = false
          })
      },
    },
    created() {
      this.getData()
    },
    watch: {
      // call again the method if the route changes
      '$route': 'getData'
    },
  };
</script>
<style lang="sass">
  @import '../assets/style/theme.sass'
  .registry
    margin: 20px 2% 0 2%

  .registry-filter
    display: flex
    flex-flow: row wrap
  .filter-item
    height: 30px
    min-width: 30px
    font-size: 18px
    font-weight: bold
    background-color: #f7f7f7
    display: flex
    align-items: center
    justify-content: center
    cursor: pointer
    margin: 10px 6px 0 0
    &:hover
      background-color: $color-brick
      color: white
</style>

