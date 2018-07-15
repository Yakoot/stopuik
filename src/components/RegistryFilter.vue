<template>
  <div>
    <div class="filter-title">Поиск по параметрам</div>
    <el-form class="filter-form" label-position="top">
      <el-row :gutter="10">
        <el-col :xs="24" :sm="4">
          <el-form-item label="Номер ТИК">
            <el-select :popper-append-to-body="false"
                       v-model="filters.tik"
                       :default-first-option="true"
                       no-match-text="Нет совпадений"
                       filterable
                       clearable
                       @change="filter"
                       placeholder="Любой">
              <el-option
                  v-for="item in data.tik"
                  :key="item"
                  :label="item"
                  :value="item">
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="4">
          <el-form-item label="Номер УИК">
            <el-select :popper-append-to-body="false"
                       v-model="filters.uik"
                       :default-first-option="true"
                       no-match-text="Нет совпадений"
                       filterable
                       clearable
                       @change="filter"
                       placeholder="Любой">
              <el-option
                  v-for="item in data.uik"
                  :key="item"
                  :label="item"
                  :value="item">
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="4">
          <el-form-item label="Год нарушения">
            <el-select :popper-append-to-body="false"
                       v-model="filters.year"
                       :default-first-option="true"
                       no-match-text="Нет совпадений"
                       filterable
                       clearable
                       @change="filter"
                       placeholder="Любой">
              <el-option
                  v-for="item in data.year"
                  :key="item"
                  :label="item"
                  :value="item">
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="Вид нарушения">
            <el-select :popper-append-to-body="false"
                       v-model="filters.report"
                       :default-first-option="true"
                       no-match-text="Нет совпадений"
                       filterable
                       clearable
                       @change="filter"
                       placeholder="Любое">
              <el-option
                  v-for="item in data.report"
                  :key="item"
                  :label="item"
                  :value="item">
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row class="name_input">
        <el-col>
          <el-input clearable placeholder="поиск по фамилиям, именам и отчествам" @input.native="input"
                    v-model="filters.name"></el-input>
        </el-col>
      </el-row>
    </el-form>
    <mq-layout mq="sm">
      <transition name="fade">
        <div class="search-count search-count-sm" v-if="isEmpty">
          <div>Найдено {{searchLength}}</div>
          <el-button @click.native="resetFilter()">Сбросить фильтр</el-button>
        </div>
      </transition>
    </mq-layout>
    <mq-layout mq="md+">
      <transition name="fade">
        <div class="search-count search-count-md" v-if="isEmpty">
          <div>Найдено {{searchLength}}</div>
          <el-button @click.native="resetFilter()">Сбросить фильтр</el-button>
        </div>
      </transition>
    </mq-layout>
  </div>
</template>
<script>
  export default {
    data() {
      return {
        filters: {
          uik: "",
          tik: "",
          year: "",
          report: "",
          name: ""
        },
        delayTimer: null
      }
    },
    props: ["data", "searchLength"],
    computed: {
      isEmpty() {
        const notEmpty = Object.keys(this.filters).reduce((sum, key) => {
          if (this.filters[key] !== "") {
            sum++
          }
          return sum
        }, 0);
        return notEmpty !== 0
      }
    },
    created() {
    },
    methods: {
      filter() {
        this.$emit("filter", {...this.filters})
      },
      input() {
        clearTimeout(this.delayTimer);
        this.delayTimer = setTimeout(() => {
          this.filter()
        }, 1000);
      },
      resetFilter() {
        for (let key in this.filters) {
          this.filters[key] = ""
        }
        this.filter()
      }
    }
  };
</script>
<style lang="scss" scoped>
  @import '../assets/style/theme';

  .filter-title {
    font-size: 18px;
    color: #000000;
    margin: 30px 0 20px 0;
    text-align: left;
  }

  .filter-form /deep/ {
    .el-input__inner {
      background-color: #f7f7f7
    }
    .name_input {
      .el-input__inner {
        border-radius: 20px;
      }
    }
    .el-form-item {
      text-align: left;
      .el-form-item__content {
        line-height: normal !important;
      }
      .el-form-item__label {
        line-height: normal !important;
      }
      .el-select {
        display: block;
      }
    }
  }

  .fade-enter-active, .fade-leave-active {
    transition: opacity .5s;
  }

  .fade-enter, .fade-leave-to /* .fade-leave-active до версии 2.1.8 */
  {
    opacity: 0;
  }

  .search-count {
    margin: 40px 0;
    display: flex;

    &.search-count-sm {
      flex-direction: column;
      font-size: 24px;
      button {
        width: 100%;
        margin-top: 20px;
      }
    }
    &.search-count-md {
      font-size: 36px;
      text-align: left;
      button {
        margin-left: 20px;
      }
    }
  }
</style>
