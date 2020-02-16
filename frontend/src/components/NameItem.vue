<template>
  <el-collapse-item class="person" :name="item.id">
    <template slot="title">
      <div class="name-item">
        <div class="name">{{ item.name }}</div>
        <!--
        <div v-if="item.current_status" class="status"><span class="status-year">Сейчас: </span>{{getStatusString(item.current_status)}}
          <el-tooltip v-if="item.current_status.from" class="current-from" :content="item.current_status.from" placement="top">
            <i class="el-icon-info"></i>
          </el-tooltip>
        </div>
        -->
        <div v-for="status in item.status" class="status"><span class="status-year">{{status.year}}: </span>{{getStatusString(status)}}</div>
      </div>
    </template>
    <i class="el-icon-caret-top"></i>
    <div v-for="(yearViolations, year) in item.violations" :key="year" class="violations">
      <div class="violations-title">Нарушения за {{year}} год</div>
      <el-collapse v-model="activeViolations">
        <div
            v-for="(violation, index) in yearViolations"
            :key="year + '_' + index"
            class="violation">

          <el-collapse-item :name="year + '_' + index">
            <template slot="title">
              <div class="violation-description">{{ violation.description }}<i :class="activeViolations.includes(year + '_' + index)  ? 'el-icon-caret-top' : 'el-icon-caret-bottom'"></i></div>
            </template>
            <div class="violation-link"
              v-for="(link_item, index) in violation.links"
              :key="index"
              v-if="link_item.link"><a :href="link_item.link" target="_blank">{{ link_item.link_description === "" ? "Источник" : link_item.link_description }}</a></div>
          </el-collapse-item>
        </div>
      </el-collapse>
    </div>
  </el-collapse-item>
</template>
<script lang="ts">
  import {Prop, Vue, Watch} from "vue-property-decorator";
  import {Component} from "vue-property-decorator";
  import {Crime, SearchResult, UikMemberStatus} from "./Model";

  @Component
 export default class NamedItem extends Vue {
    private activeViolations = [];

    @Prop(Object) item: SearchResult | undefined;

    getStatusString(status: UikMemberStatus) {
      let str = "";
      if (status.uik_status) {
        str += `${status.uik_status}`;
      }
      if (status.uik) {
        str += ` УИК ${status.uik},`;
      }
      if (status.tik) {
        str += ` ТИК ${status.tik}`;
      }
      return str;
    }

    // get violations(): {[key: string]: Array<Crime>} {
    //   return this.item!!.violations;
    // }
  };
</script>
<style lang="scss">
  @import '../assets/style/theme';

  .el-tooltip__popper {
    max-width: 200px;
    background-color: $color-brick;
  }
  .current-from-tooltip {
    background-color: $color-brick;
  }
  .current-from {
    margin-left: 5px;
  }

  .name-item {
    line-height: normal;
  }
  .name {
    color: $color-brick;
    font-size: 16px;
    line-height: 1.25;
  }
  .status {
    color: #9b9b9b;
    font-size: 12px;
    line-height: 1.5;
    .status-year {
      font-weight: bold;
    }
  }
  .violations {
    padding: 20px;
    background-color: $color-brick !important;
  }

  .violations-title {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.4);
  }

  .person {
    width: 300px;
    margin-bottom: 20px !important;
    > .el-collapse-item__wrap {
      > .el-collapse-item__content {
        position: relative;
        padding-bottom: 25px;
        margin-top: 10px;
        color: white;
        > .el-icon-caret-top {
          color: $color-brick;
          position: absolute;
          top: -19px;
          left: 20px;
          font-size: 31px;
        }
      }
    }
  }
  .el-collapse {
    border: none !important;
    .el-collapse-item__header {
      height: unset !important;
      line-height: unset;
      background-color: unset;
      color: inherit;
      cursor: pointer;
      border-bottom: none !important;
      font-size: unset;
      font-weight: unset;
      transition: unset;
    }
    .el-collapse-item__wrap {
      border: none;
    }
    .el-collapse-item__arrow {
      display: none !important;
    }

    .el-collapse-item__content {
      padding-bottom: 0;
      margin-top: 10px;
    }
  }

  .violation {
    margin-top: 15px;
    font-size: 12px;
    .el-collapse-item__wrap {
      background-color: $color-brick !important;
    }
    .violation-description {
      font-size: 14px;
      color: #fff;
    }
    .violation-link {
      margin: 10px 0 0 20px;
      font-size: 12px;
      color: #fff;
      a {
        color: #fff;
        cursor: pointer;
        &:hover {
          border-bottom: 1px solid;
        }
      }
    }
  }
</style>
