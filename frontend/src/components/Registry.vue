<template>
    <div class="registry">
        <el-alert
                v-if="errorActive"
                :title="errorTitle"
                type="error"
                :description="errorDetails"
                show-icon
                @close="clearError">
        </el-alert>
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
  import axios, {AxiosError, AxiosPromise} from "axios";

  import RegistryFilter from './RegistryFilter.vue'
  import LetterBlock from './LetterBlock.vue'
  import {Component, Vue, Watch} from "vue-property-decorator";
  import {FilterData, SearchQuery, SearchResponse, SearchResult, UikCrimeResponse} from "./Model";

  @Component({
    components: {
      RegistryFilter, LetterBlock
    },
  })
  export default class Registry extends Vue {
    private data: Array<SearchResult> = [];
    private filterData: FilterData = {};
    private letters: Array<String> = [];
    private loading = true;
    private filterLoading = false;
    private activeName: Array<String> = [];
    private searchParams: SearchQuery = {};
    private searchLength: number = 0;
    private items: {[key: string]: Array<SearchResult>} = {};
    private id2result: {[key: number]: SearchResult} = {};
    private errorActive = false;
    private errorTitle = "";
    private errorDetails = "";
    private httpClient = axios.create({
      timeout: 10000,
    });
    created() {
      this.getData()
    }

    fetchViolations(activeItem: string) {
      let activeUikMemberId = parseInt(activeItem);
      this.httpClient.post<UikCrimeResponse>(
          "/_ah/api/blacklist/v1/uik_crime", {
            uik_member_id: activeUikMemberId
          }
      ).then(response => {
        let searchResult = this.id2result[activeUikMemberId];
        if (searchResult) {
          searchResult.violations = response.data.violations;
        }
      }).catch(this.handleError);
    }

    private handleError(error: AxiosError) {
      this.errorActive = true;
      this.errorTitle = "Что-то пошло не так";
      if (error.response) {
        this.errorDetails = `
HTTP ${error.response.status}: ${error.response.statusText}\n
(при выполнении запроса: ${error.config.url})
        `;
      } else {
        this.errorDetails = `Не получен ответ (при выполнении запроса: ${error.config.url})`;
      }
    }

    @Watch("$route")
    getData() {
      this.httpClient.get<FilterData>("/filters.json")
          .then(response => {
            let intLessThan = function (a: string, b: string): number {
              return parseInt(a) - parseInt(b);
            };
            if (response.data.ikmo) {
              response.data.ikmo.sort();
              this.filterData.ikmo = response.data.ikmo;
            }
            if (response.data.uik) {
              response.data.uik.sort(intLessThan);
              this.filterData.uik = response.data.uik;
            }
            if (response.data.tik) {
              response.data.tik.sort(intLessThan);
              this.filterData.tik = response.data.tik;
            }
            if (response.data.year) {
              response.data.year.sort(intLessThan);
              this.filterData.year = response.data.year;
            }
            if (response.data.report) {
              response.data.report.sort();
              this.filterData.report = response.data.report;
            }
            this.filterData = {... this.filterData, ...response.data};
            this.loading = false;
          }).catch(this.handleError);
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
      this.search().catch(() => {
        this.filterLoading = false;
      });
    }

    createLetters(data: Array<SearchResult>): { [key: string]: Array<SearchResult>; } {
      let newData: { [key: string]: Array<SearchResult>; } = {};
      data.forEach(item => {
        const letter = item.name.charAt(0);
        if (Object.keys(newData).indexOf(letter) < 0) {
          newData[letter] = [];
        }
        newData[letter].push(item);
        item.violations = {};
      });
      this.filterLoading = false;
      return newData;
    }

    search(): AxiosPromise<SearchResponse> {
      const result = this.httpClient.post<SearchResponse>(
          "/_ah/api/blacklist/v1/search", this.searchParams
      );
      result.then(xhr => {
        this.id2result = {};

        if (xhr.data.data) {
          this.items = this.createLetters(xhr.data.data);
          xhr.data.data.forEach(result => {
            this.id2result[result.id] = result;
          });
          this.searchLength = xhr.data.data.length;
        } else {
          this.items = {};
          this.searchLength = 0;
        }

        this.filterLoading = false;
      }).catch(this.handleError);
      return result;
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
    }

    clearError() {
      this.errorActive = false;
      this.errorTitle = "";
      this.errorDetails = "";
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

