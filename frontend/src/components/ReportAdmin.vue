import {UikType} from "@/components/Model";
<template>
    <el-dialog
            title="Административный интерфейс"
            :visible.sync="visible"
            :before-close="handleClose"
            fullscreen
            center>
        <el-form ref="form" :model="form" label-width="20em">
            <el-form-item label="Избирательная комиссия">
                <el-select
                        v-model="form.uik"
                        :disabled="!allUiksLoaded"
                        filterable
                        placeholder="Где нарушение?">
                    <el-option
                            v-for="item in allUiks"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value">
                    </el-option>
                </el-select>
            </el-form-item>
            <el-form-item label="Член комиссии">
                <el-select
                        v-model="form.selectedUikMember"
                        :disabled="!uikMembersLoaded"
                        multiple
                        filterable
                        allow-create
                        default-first-option
                        placeholder="Кто нарушитель?">
                    <el-option
                            v-for="item in uikMembers"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value">
                    </el-option>
                </el-select>
            </el-form-item>
            <el-form-item label="Тип нарушения">
                <el-select
                        v-model="form.crimeType"
                        filterable
                        default-first-option
                        placeholder="Тип нарушения">
                    <el-option
                            v-for="item in crimeTypes"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value">
                    </el-option>
                </el-select>
            </el-form-item>
            <el-form-item
                    v-for="(link, index) in form.links"
                    :label="'Ссылка №' + (index+1)"
                    :prop="'links.' + index + '.value'"
                    class="link-row"
            >
                <el-input v-model="link.title" class="link-title"></el-input>
                <el-input v-model="link.url" class="link-url"></el-input>

                <el-button @click.prevent="removeLink(link)">Удалить</el-button>
            </el-form-item>

            <el-form-item>
                <el-button round @click="addLink">Добавить ссылку</el-button>
            </el-form-item>

            <el-form-item>
                <el-button type="primary" @click="onSubmit">Создать запись</el-button>
                <el-button>Cancel</el-button>
            </el-form-item>
        </el-form>
        <p>uik={{form.uik}} member={{form.selectedUikMember}}</p>
    </el-dialog>
</template>
<script lang="ts">
  import {Component, Prop, Vue, Watch} from "vue-property-decorator";
  import axios, {AxiosError} from "axios";
  import {
    AllUiksQuery,
    AllUiksResponse,
    FilterData,
    UikMembersQuery,
    UikMembersResponse,
    UikType
  } from "@/components/Model";

  interface UikDropdownItem {
    value: number;
    label: string;
  }

  interface UikMemberDropdownItem {
    value: number;
    label: string;
  }

  interface CrimeTypeDropdownItem {
    value: string;
    label: string;
  }

  interface LinkItem {
    url: string,
    title: string
  }

  @Component({
  })
  export default class ReportAdmin extends Vue {
    @Prop(Boolean) readonly visible?: boolean;
    private httpClient = axios.create({
      timeout: 10000,
    });

    private form = {
      uik: undefined,
      selectedUikMember: undefined,
      crimeType: undefined,
      links: [] as Array<LinkItem>
    };

    private allUiks: Array<UikDropdownItem> = [];
    private allUiksLoaded = false;
    private uikMembersLoaded = false;
    private uikMembers: Array<UikMemberDropdownItem> = [];
    private crimeTypes: Array<CrimeTypeDropdownItem> = [];

    close() {
        this.$emit("closeReport");
    }
    handleClose() {
        this.$emit("closeReport");
    }
    onSubmit() {
        console.log("!!!!!!!!!!!");
    }

    addLink() {
      this.form.links.push({title: "", url: "http://"})
    }
    removeLink(link: LinkItem) {
      this.form.links = this.form.links.filter(item => item !== link);
    }

    @Watch("visible")
    onVisibleChange() {
      this.loadUiks({
        year: 2019
      });
      if (window.filters) {
        this.crimeTypes = ((window.filters as FilterData).report || []).map(item => {
          return {
            value: item,
            label: item
          }
        });
      }
    }

    @Watch("form.uik")
    onUikNumberChange() {
      this.loadUikMembers({
        uik: this.form.uik || 0,
        year: 2019
      });
    }

    private loadUiks(query: AllUiksQuery) {
      this.httpClient.post<AllUiksResponse>("/_ah/api/blacklist/v1/all_uiks", query).then(response => {
        this.allUiks = response.data.uiks.map(item => {
          return {
            value: item.id,
            label: `${formatUikType(item.type)} ${item.name}`
          }
        });
        this.allUiksLoaded = true;
      }).catch((error: AxiosError) => {
        console.error(error);
      });
    }

    private loadUikMembers(query: UikMembersQuery) {
        this.httpClient.post<UikMembersResponse>("/_ah/api/blacklist/v1/uik_members", query).then(response => {
            this.uikMembers = response.data.people.map(item => {
              return {
                value: item.id,
                label: `[${item.status}] ${item.name}`
              }
            });
            this.uikMembersLoaded = true;
        }).catch((error: AxiosError) => {
          console.error(error);
        });
    }
  }

  function formatUikType(type: UikType) {
    switch (type) {
      case "UIK": return "УИК";
      case "IKMO": return "ИКМО";
      case "TIK": return "ТИК";
    }
  }
</script>
<style lang="scss">
    .el-select {
        width: 100%;
    }
    .link-row {
        .link-title {
            margin-bottom: 0.75ex;
        }
        .link-url {
            display: inline-block;
            width: 75%;
            margin-right: 0.5em;
        }
        margin-bottom: 1em;
    }
</style>
