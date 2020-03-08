import {UikType} from "@/components/Model";
<template>
    <div>
        <el-dialog  v-if="authState === 1"
                    title="Вход для редакторов"
                    visible
                    :before-close="handleClose"
                    center>
            <el-alert
                    v-if="errorActive"
                    :title="errorTitle"
                    type="error"
                    :description="errorDetails"
                    show-icon
                    @close="clearError"></el-alert>
            <el-form ref="authForm" :model="authForm" @submit.native.prevent="login">
                <el-form-item prop="username">
                    <el-input v-model="authForm.email" placeholder="Адрес электронной почты" prefix-icon="fas fa-user"></el-input>
                </el-form-item>
                <el-form-item prop="password">
                    <el-input
                            v-model="authForm.password"
                            placeholder="Пароль"
                            type="password"
                            prefix-icon="fas fa-lock"
                    ></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button
                            class="login-button"
                            type="primary"
                            native-type="submit"
                            block>Тук Тук</el-button>
                </el-form-item>
            </el-form>
        </el-dialog>
    <el-dialog  v-if="authState === 0"
            title="Редакторский интерфейс"
            visible
            :before-close="handleClose"
            fullscreen
            center>
        <el-alert
                v-if="errorActive"
                :title="errorTitle"
                type="error"
                :description="errorDetails"
                show-icon
                @close="clearError">
        </el-alert>
        <el-form ref="form" :model="form" :rules="validationRules" label-width="20em" v-loading="isLoading">
            <el-form-item label="Избирательная комиссия">
                <el-select
                        v-model="form.uik"
                        :disabled="!allUiksLoaded"
                        filterable
                        placeholder="Где нарушение?"
                        id="uik-chooser">
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
                        v-model="form.uikMembers"
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
                    :key="'crimeLink' + index"
                    :prop="'crimeLink' + (index)"
                    :rules="validationRules.crimeLink"
                    class="link-row">
                <el-input v-model="link.title" class="link-title"></el-input>
                <el-input v-model="link.url" class="link-url" :id="'crimeLink' + index"></el-input>

                <el-button @click.prevent="removeLink(link)">Удалить</el-button>
            </el-form-item>

            <el-form-item>
                <el-button round @click="addLink">Добавить ссылку</el-button>
            </el-form-item>

            <el-form-item>
                <el-button type="primary" @click="onSubmit" :disabled="!submitEnabled">Создать запись</el-button>
            </el-form-item>
        </el-form>
        <el-alert
                v-if="successActive"
                :title="successTitle"
                type="success"
                :description="successDetails"
                show-icon
                @close="clearSuccess">
        </el-alert>
        <p>uik={{form.uik}} member={{form.uikMembers}} crime={{form.crimeType}} links={{form.links}}</p>
    </el-dialog>
    </div>
</template>
<script lang="ts">
  import {Component, Prop, Vue, Watch} from "vue-property-decorator";
  import axios, {AxiosError} from "axios";
  import {
    AllUiksQuery,
    AllUiksResponse, CreateCrimeRequest, CreateCrimeResponse,
    FilterData,
    UikMembersQuery,
    UikMembersResponse,
    UikType
  } from "@/components/Model";
  import Axios from "axios";

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

  interface FormData {
    uik?: number;
    uikMembers?: Array<number | string>;
    crimeType?: string;
    links: Array<LinkItem>;
  }

  enum AuthState { OK, NOT_AUTHENTICATED, UNKNOWN }
  @Component({
  })
  export default class ReportAdmin extends Vue {
    @Prop(Boolean) readonly visible?: boolean;
    private httpClient = axios.create({
      timeout: 10000,
    });

    private authForm = {
      password: "",
      email: ""
    };

    private form: FormData = {
      links: [{title: "", url: ""}]
    };

    private validationRules = {
        crimeLink: [
          {validator: this.validateCrimeLink, trigger: 'blur'}
        ]
    };

    private authState: AuthState = AuthState.UNKNOWN;
    private isLoading = false;
    private allUiks: Array<UikDropdownItem> = [];
    private allUiksLoaded = false;
    private uikMembersLoaded = false;
    private uikMembers: Array<UikMemberDropdownItem> = [];
    private crimeTypes: Array<CrimeTypeDropdownItem> = [];
    private submitEnabled = false;
    private errorActive = false;
    private errorTitle = "";
    private errorDetails = "";
    private successActive = false;
    private successTitle = "";
    private successDetails = "";

    mounted() {
      this.loadAuthState();
      this.loadUiks({
        year: 2019
      });
      this.loadCrimeTypes();
      window.setTimeout(() => {
        const input = document.querySelector("#uik-chooser") as HTMLInputElement;
        if (input) {
          input.focus();
        }
      }, 1000);
    }
    login() {
      const data = `email=${encodeURIComponent(this.authForm.email)}&password=${encodeURIComponent(this.authForm.password)}`;
      axios({
        url: "/_ah/signin/do",
        method: "POST",
        data: data,
        headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8' }
      }).then(response => {
        if (response.status === 200) {
          this.loadAuthState();
        } else {
          this.errorActive = true;
          this.errorTitle = "Что-то пошло не так";
          this.errorDetails = response.statusText;
        }
      });
    }

    loadAuthState() {
      this.httpClient.get("/_ah/signin/check").then(response => {
        switch (response.status) {
          case 200: {
            this.authState = AuthState.OK;
            break;
          }
          case 401: {
            this.authState = AuthState.NOT_AUTHENTICATED;
          }
          default: {
            this.errorActive = true;
            this.errorTitle = "Что-то пошло не так";
            this.errorDetails = response.statusText;
          }
        }
      }).catch((error: AxiosError) => {
        if (error.response) {
          if (error.response.status === 401) {
            this.authState = AuthState.NOT_AUTHENTICATED;
          }
        }
      });
    }

    close() {
      //window.location.pathname = "/";
      this.$router.push("/");
    }
    handleClose() {
      this.$router.push("/");
    }
    onSubmit() {
      if (this.form.uik === undefined || this.form.uikMembers === undefined || this.form.crimeType === undefined) {
        return;
      }
      const query: CreateCrimeRequest = {
        uik: this.form.uik,
        uikMembers: this.form.uikMembers.filter(it => typeof it === "number") as Array<number>,
        newUikMembers: this.form.uikMembers.filter(it => typeof it === "string") as Array<string>,
        crimeType: this.form.crimeType,
        crimeLinks: this.form.links
      };
      this.isLoading = true;
      this.httpClient.post<CreateCrimeResponse>("/_ah/api/blacklist/v1/create_crime", query).then(response => {
        if (response.data.crimeId > 0) {
            this.successTitle = "Нарушение опубликовано";
            this.successDetails = response.data.message;
            this.successActive = true;
            window.setInterval(() => this.clearSuccess(), 3000);
        } else {
          this.errorTitle = "Что-то пошло не так";
          this.errorDetails = `Публикация нарушения завершилась с ошибкой: ${response.data.message}`
          this.errorActive = true;
        }
        this.isLoading = false;
      }).catch(this.handleError);
    }

    addLink() {
      this.form.links.push({title: "", url: "http://"})
    }
    removeLink(link: LinkItem) {
      this.form.links = this.form.links.filter(item => item !== link);
    }

    @Watch("visible")
    onVisibleChange() {
    }

    @Watch("form.uik")
    onUikNumberChange() {
      this.loadUikMembers({
        uik: this.form.uik || 0,
        year: 2019
      });
    }

    validateCrimeLink(rule: any, value: any, callback: (err: Error) => any) {
        const linkInput = document.getElementById(rule.field);
        if (linkInput && linkInput instanceof HTMLInputElement) {
          if (!isUrl(linkInput.value)) {
            callback(Error("Ссылка должна выглядеть примерно так: https://example.com"));
          }
        }
        this.validateForm();
    }

    @Watch("form.uik")
    @Watch("form.uikMembers")
    @Watch("form.crimeType")
    @Watch("form.links")
    validateForm() {
      this.submitEnabled = this.form.uik !== undefined
          && this.form.uikMembers !== undefined
          && this.form.uikMembers.length > 0
          && this.form.crimeType !== undefined
          && this.form.links.length > 0
          && this.form.links.filter(it => isUrl(it.url)).length > 0;
    }

    private loadUiks(query: AllUiksQuery) {
      this.isLoading = true;
      this.httpClient.post<AllUiksResponse>("/_ah/api/blacklist/v1/all_uiks", query).then(response => {
        this.allUiks = response.data.uiks.map(item => {
          return {
            value: item.id,
            label: `${formatUikType(item.type)} ${item.name}`
          }
        });
        this.allUiksLoaded = true;
        this.isLoading = false;
      }).catch(this.handleError);
    }

    private loadCrimeTypesFromFilters(filters: FilterData) {
      this.crimeTypes = (filters.report || []).map(item => {
        return {
          value: item,
          label: item
        }
      });
    }
    private loadCrimeTypes() {
      if (!window.filters) {
        this.httpClient.get<FilterData>("/filters.json")
            .then(response => this.loadCrimeTypesFromFilters(response.data))
            .catch(this.handleError);
      } else {
        this.loadCrimeTypesFromFilters((window.filters as FilterData))
      }
    }

    private loadUikMembers(query: UikMembersQuery) {
      this.isLoading = true;
        this.httpClient.post<UikMembersResponse>("/_ah/api/blacklist/v1/uik_members", query).then(response => {
          if (response.data.people) {
            this.uikMembers = response.data.people.map(item => {
              return {
                value: item.id,
                label: `${formatStatus(item.status)} ${item.name}`
              }
            });
          } else {
            this.uikMembers = [];
          }
          this.uikMembersLoaded = true;
          this.isLoading = false;
        }).catch(this.handleError);
    }

    private handleError(error: AxiosError) {
      this.isLoading = false;
      this.errorActive = true;
      this.errorTitle = "Что-то пошло не так";
      if (error.response) {
        this.errorDetails = `
HTTP ${error.response.status}: ${error.response.statusText}\n
(при выполнении запроса: ${error.config.url})
        `;
      } else {
        if (error.config) {
          this.errorDetails = `Не получен ответ (при выполнении запроса: ${error.config.url})`;
        } else {
          this.errorDetails = error.message;
        }
      }
    }
    clearError() {
      this.errorActive = false;
      this.errorTitle = "";
      this.errorDetails = "";
    }
    clearSuccess() {
      this.successActive = false;
      this.successTitle = "";
      this.successDetails = "";
    }

  }

  function formatUikType(type: UikType): string {
    switch (type) {
      case "UIK": return "УИК";
      case "IKMO": return "ИКМО";
      case "TIK": return "ТИК";
    }
  }

  function formatStatus(status: number): string {
    switch (status) {
        case 1: return "Пред.";
        case 2: return "Зам.";
        case 3: return "Секр.";
        case 4: return "ЧПРГ";
        case 5: return "ЧПСГ";
        default: return `[${status}]`;
    }
  }
  function isUrl(url: string) {
    return url.trim().startsWith("http://") || url.trim().startsWith("https://");
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
    .el-button.is-disabled {
        opacity: 0.5;
    }
</style>
