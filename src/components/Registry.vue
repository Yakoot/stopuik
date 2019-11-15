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
<script lang="ts">
  import axios from "axios";

  import RegistryFilter from './RegistryFilter.vue'
  import LetterBlock from './LetterBlock.vue'
  import {Component, Vue, Watch} from "vue-property-decorator";

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

  interface SearchResult {
    filter_data: FilterData;
    name: string;
  }

  interface SearchQuery {
    ikmo?: string;
    report?: string;
    name?: string;
    year?: string;
    uik?: string;
    tik?: string;
  }

  export interface FilterData {
    description?: Array<String>;
    ikmo?: Array<String>;
    year?: Array<String>;
    uik?: Array<String>;
    tik?: Array<String>;
    report?: Array<String>;
  }

  export interface DataResponse {
    data: Array<SearchResult>;
    filterData: FilterData;
  }

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
    private searchLength?: number;

    get items() {
      let data = this.data;
      data = this.search(data);
      return this.createLetters(data);
    }

    created() {
      this.getData()
    }

    @Watch("$route")
    getData() {
      axios.get<DataResponse>("/data.json")
          .then(response => {
            this.data = response.data.data;
            this.filterData = {...this.filterData, ...response.data.filterData};
            this.loading = false;
          });
    }

    filter(data: SearchQuery) {
      this.filterLoading = true;
      this.searchParams = data;
    }

    createLetters(data: Array<SearchResult>): { [key: string]: Array<SearchResult>; } {
      let newData: { [key: string]: Array<SearchResult>; } = {};
      data.forEach(item => {
        const letter = item.name.charAt(0);
        if (!Object.keys(newData).includes(letter)) {
          newData[letter] = [];
        }
        newData[letter].push(item)
      });
      this.filterLoading = false;
      return newData;
    }

    search(data: Array<SearchResult>): Array<SearchResult> {
      let newData = data;
      if (this.searchParams.tik) {
        const tik = this.searchParams.tik;
        newData = newData.filter(item => {
          if (item.filter_data.tik) return item.filter_data.tik.includes(tik);
          else return false;
        })
      }
      if (this.searchParams.uik) {
        const uik = this.searchParams.uik;
        newData = newData.filter(item => {
          if (item.filter_data.uik) return item.filter_data.uik.includes(uik);
          else return false;
        })
      }
      if (this.searchParams.year) {
        const year = this.searchParams.year;
        newData = newData.filter(item => {
          if (item.filter_data.year) return item.filter_data.year.includes(year);
          else return false;
        })
      }
      if (this.searchParams.name) {
        const name = this.searchParams.name;
        newData = newData.filter(item => {
          return item.name.toLowerCase().includes(name.toLowerCase())
        })
      }
      if (this.searchParams.report) {
        const report = this.searchParams.report;
        newData = newData.filter(item => {
          if (item.filter_data.description) return item.filter_data.description.includes(report);
          else return false;
        })
      }
      if (this.searchParams.ikmo) {
        const ikmo = this.searchParams.ikmo;
        newData = newData.filter(item => {
          if (item.filter_data.ikmo) return item.filter_data.ikmo.includes(ikmo);
          else return false;
        })
      }
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

