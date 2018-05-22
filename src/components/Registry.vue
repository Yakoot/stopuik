<template>
  <div class="registry"
       v-loading="loading">
    <RegistryFilter
        :data="letters"
        @select-letter="selectLetter"/>
    <LetterBlock
        v-for="(item, key) in items"
        :key="key"
        :letter="key"
        :items="item"/>
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
        activeLetter: "",
        loading: true
      };
    },
    computed: {
      items() {
        if (this.activeLetter !== "") {
          const data = {};
          data[this.activeLetter] = this.data[this.activeLetter];
          return data;
        }
        return this.data
      }
    },
    props: [],
    methods: {
      getData() {
        axios.get("http://registry.tbrd.ru/api.php", {
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
      selectLetter(letter) {
        this.activeLetter = letter
      }
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
  .registry
    margin-left: 40px
</style>

