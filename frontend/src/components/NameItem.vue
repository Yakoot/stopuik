<template>
  <el-collapse-item class="person" :name="item.id">
    <template slot="title">
      <el-card shadow="hover" class="name-item">
        <div slot="header" class="clearfix">
          <span class="name">{{item.name}}</span>
        </div>
        <div v-for="status in item.status">
          <el-row class="status">
            <el-col :span="4" class="status-year">{{status.year}}</el-col>
            <el-col :span="20">{{getStatusString(status)}}</el-col>
          </el-row>
          <el-row class="status-managing">
            <el-col :span="20" :offset="4">{{getManagingUikString(status)}}</el-col>
          </el-row>
        </div>
      </el-card>
<!--      <div class="name-item">-->
<!--        <div class="name">{{ item.name }}</div>-->
<!--        <div v-for="status in item.status">-->
<!--          <el-row class="status">-->
<!--            <el-col :span="4" class="status-year">{{status.year}}</el-col>-->
<!--            <el-col :span="20">{{getStatusString(status)}}</el-col>-->
<!--          </el-row>-->
<!--          <el-row class="status-managing">-->
<!--            <el-col :span="20" :offset="4">{{getManagingUikString(status)}}</el-col>-->
<!--          </el-row>-->
<!--        </div>-->
<!--      </div>-->
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
  import {AllUiksResponseItem, Crime, formatUikLabel, SearchResult, UikMemberStatus} from "./Model";

  @Component
 export default class NamedItem extends Vue {
    private activeViolations = [];

    @Prop(Object) item: SearchResult | undefined;

    getStatusString(status: UikMemberStatus): string {
      let str = "";
      if (status.uik_status) {
        str += `${status.uik_status} `;
      }
      if (status.uik) {
        str += formatUikLabel(window.allManagingUiks[status.uik] as AllUiksResponseItem);
      }
      return str;
    }

    getManagingUikString(status: UikMemberStatus): string {
      if (status.tik) {
        return formatUikLabel(window.allManagingUiks[status.tik] as AllUiksResponseItem)
      } else {
        return "";
      }
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
    border: none !important;
    padding: 20px;
    width: 100%;
    line-height: normal;

    .el-card__header {
      padding: 0;
      border-bottom: none;
    }
    .el-card__body {
      padding: 0;
    }
  }
  .name {
    color: $color-brick;
    font-size: 110%;
    line-height: 1.25;
  }
  .status {
    color: #9b9b9b;
    font-size: 100%;
    line-height: 1.5;
    .status-year {
      font-weight: bold;
      text-align: right;
      padding-right: 0.5em;
    }
  }
  .status-managing {
    color: #9b9b9b;
    font-size: 80%;
    line-height: 1.5;
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
    width: 100%;
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
