<template>
  <div class="registry">
    <RegistryFilter @filter="filter" :data="filterData" :search-length="searchLength"></RegistryFilter>
    <div v-loading="loading || filterLoading">
      <el-collapse accordion v-model="activeName">
        <LetterBlock
            v-for="(item, key) in items"
            :key="key"
            :letter="key"
            :items="item"/>
      </el-collapse>
    </div>
  </div>
</template>
<script>
  import axios from "axios";

  import RegistryFilter from './RegistryFilter'
  import LetterBlock from './LetterBlock'

  const reportData = [
    "досрочное голосование",
    "карусели",
    "вброс бюллетеней",
    "переписанные итоговые протоколы",
    "помещение для голосования",
    "голосование вне помещения для голосования",
    "ограничение прав членов комиссии наблюдателей представителей СМИ",
    "голосование в помещении для голосования",
    "подсчет голосов и установление итогов",
    "избирательная документация"
  ];

  export default {
    components: {RegistryFilter, LetterBlock},
    data() {
      return {
        data: [],
        filterData: {
          report: reportData
        },
        letters: [],
        loading: true,
        filterLoading: false,
        activeName: [],
        searchParams: {},
        searchLength: null,
      };
    },
    computed: {
      items() {
        let data = this.data;
        data = this.search(data);
        return this.createLetters(data)
      }
    },
    props: [],
    methods: {
      getData() {
        axios.get("https://registry.tbrd.ru/api.php", {
          params: {
            method: "getData",
            sheet: "Санкт-Петербург"
          }
        })
          .then(response => {
            this.data = response.data.data;
            this.filterData = {...this.filterData, ...response.data.filterData};
            this.loading = false
          })
      },
      filter(data) {
        this.filterLoading = true;
        this.searchParams = data
      },
      createLetters(data) {
        let newData = {};
        data.forEach(item => {
          const letter = item.name.charAt(0);
          if (!Object.keys(newData).includes(letter)) {
            newData[letter] = [];
          }
          newData[letter].push(item)
        });
        this.filterLoading = false;
        return newData
      },
      search() {
        let newData = this.data;
        if (this.searchParams.tik) {
          newData = newData.filter(item => {
            return item.tik === this.searchParams.tik
          })
        }
        if (this.searchParams.uik) {
          newData = newData.filter(item => {
            return item.uik === this.searchParams.uik
          })
        }
        if (this.searchParams.year) {
          newData = newData.filter(item => {
            return item.years.includes(this.searchParams.year)
          })
        }
        if (this.searchParams.name) {
          newData = newData.filter(item => {
            return item.name.toLowerCase().includes(this.searchParams.name.toLowerCase())
          })
        }
        if (this.searchParams.report) {
          newData = newData.filter(item => {
            return item.violations.filter(violation => {
              return violation.description === this.searchParams.report
            }).length > 0
          })
        }
        this.searchLength = newData.length;
        return newData
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
<style lang="scss">
  @import '../assets/style/theme';
  .registry {
    margin: 20px 2% 0 2%
  }

  .registry-filter {
    display: flex;
    flex-flow: row wrap;
  }

</style>

