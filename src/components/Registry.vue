<template>
    <div class="registry">
        <RegistryFilter @filter="filter" :data="filterData" :search-length="searchLength"></RegistryFilter>
        <div v-loading="loading || filterLoading">
            <el-collapse accordion v-model="activeName" @change="fetchViolations">
                <LetterBlock
                        v-for="(item, key) in items"
                        :key="key"
                        :letter="key"
                        :items="item"/>
            </el-collapse>
        </div>
    </div>
</template>
<script lang="ts">
  import axios from "axios";

  import RegistryFilter from './RegistryFilter.vue'
  import LetterBlock from './LetterBlock.vue'
  import {Component, Vue, Watch} from "vue-property-decorator";
  import {FilterData, SearchQuery, SearchResponse, SearchResult, UikCrimeResponse} from "./Model";

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

  @Component({
    components: {
      RegistryFilter, LetterBlock
    },
  })
  export default class Registry extends Vue {
    private data: Array<SearchResult> = [];
    private filterData: FilterData = {report: reportData};
    private letters: Array<String> = [];
    private loading = true;
    private filterLoading = false;
    private activeName: Array<String> = [];
    private searchParams: SearchQuery = {};
    private searchLength: number = 0;
    private items: {[key: string]: Array<SearchResult>} = {};


    created() {
      this.getData()
    }

    fetchViolations(activeItem: string) {
      axios.post<UikCrimeResponse>(
          "http://spbelect-blacklist-backend.appspot.com:8080/_ah/api/blacklist/v1/uik_crime", {
            uik_member_id: 0 + activeItem
          }
      ).then(response => {
        console.log(response);
      });
    }
    @Watch("$route")
    getData() {
      axios.get<FilterData>("http://spbelect-blacklist-backend.appspot.com:8080/_ah/api/blacklist/v1/filters")
          .then(response => {
            if (response.data.ikmo) {
              response.data.ikmo.sort();
              this.filterData.ikmo = response.data.ikmo;
            }
            if (response.data.uik) {
              response.data.uik.sort((a, b) => parseInt(a) - parseInt(b));
              this.filterData.uik = response.data.uik;
            }
            if (response.data.tik) {
              response.data.tik.sort((a, b) => parseInt(a) - parseInt(b));
              this.filterData.tik = response.data.tik;
            }
            this.filterData = {... this.filterData, ...response.data};
            this.loading = false;
          })
      // axios.get<DataResponse>("/data.json")
      //     .then(response => {
      //       this.data = response.data.data;
      //       this.filterData = {...this.filterData, ...response.data.filterData};
      //       this.loading = false;
      //     });
    }

    filter(data: SearchQuery) {
      this.filterLoading = true;
      this.searchParams = data;
      this.search([]);
    }

    createLetters(data: Array<SearchResult>): { [key: string]: Array<SearchResult>; } {
      let newData: { [key: string]: Array<SearchResult>; } = {};
      data.forEach(item => {
        const letter = item.name.charAt(0);
        if (Object.keys(newData).indexOf(letter) < 0) {
          newData[letter] = [];
        }
        newData[letter].push(item)
      });
      this.filterLoading = false;
      return newData;
    }

    search(data: Array<SearchResult>): Array<SearchResult> {
      let newData: Array<SearchResult> = [];
      axios.post<SearchResponse>(
          "http://spbelect-blacklist-backend.appspot.com:8080/_ah/api/blacklist/v1/search", this.searchParams
      ).then(xhr => {
        this.items = this.createLetters(xhr.data.data);
        this.filterLoading = false;
      });
      // if (this.searchParams.tik) {
      //   const tik = this.searchParams.tik;
      //   newData = newData.filter(item => {
      //     if (item.filter_data.tik) return item.filter_data.tik.includes(tik);
      //     else return false;
      //   })
      // }
      // if (this.searchParams.uik) {
      //   const uik = this.searchParams.uik;
      //   newData = newData.filter(item => {
      //     if (item.filter_data.uik) return item.filter_data.uik.includes(uik);
      //     else return false;
      //   })
      // }
      // if (this.searchParams.year) {
      //   const year = this.searchParams.year;
      //   newData = newData.filter(item => {
      //     if (item.filter_data.year) return item.filter_data.year.includes(year);
      //     else return false;
      //   })
      // }
      // if (this.searchParams.name) {
      //   const name = this.searchParams.name;
      //   newData = newData.filter(item => {
      //     return item.name.toLowerCase().includes(name.toLowerCase())
      //   })
      // }
      // if (this.searchParams.report) {
      //   const report = this.searchParams.report;
      //   newData = newData.filter(item => {
      //     if (item.filter_data.description) return item.filter_data.description.includes(report);
      //     else return false;
      //   })
      // }
      // if (this.searchParams.ikmo) {
      //   const ikmo = this.searchParams.ikmo;
      //   newData = newData.filter(item => {
      //     if (item.filter_data.ikmo) return item.filter_data.ikmo.includes(ikmo);
      //     else return false;
      //   })
      // }
      this.searchLength = newData.length;
      return newData;
    }
  }
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

