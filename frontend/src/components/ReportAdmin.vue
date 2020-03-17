import {UikType} from "@/components/Model";
<template>
    <div>
        <div class="header">
            <div class="header-title">
                <div class="logo-row">
                    <div class="first-title">РЕЕСТР НАРУШЕНИЙ</div>
                    <img src="../assets/images/logo-head.png" height="20">
                </div>
                <div class="second-title">Редакторский интерфейс</div>
                <div class="second-title"><el-link href="/" icon="el-icon-back">Назад к поиску</el-link></div>
            </div>
            <div>
            <div class="second-title">{{userName}} <el-button type="danger" round size="small" @click="signOut">Выйти</el-button></div>
            <div class="third-title">{{userEmail}}</div>
            </div>
        </div>
        <el-alert
                v-if="authState === 3"
                :title="'Подтвердите адрес электронной почты'"
                type="warning"
                :description="'Мы послали вам письмо со ссылкой для подтверждения адреса электронной почты. Пожалуйста, найдите письмо в почте и пройдите по ссылке.'"
                show-icon
                @close="handleClose">
        </el-alert>
        <el-alert
                v-if="authState === 4"
                :title="'Кажется, у вас нет редакторских прав'"
                type="error"
                show-icon
                @close="handleClose">
            <div>К сожалению, похоже что вы не зарегистрированы у нас как редактор. Если вы считаете, что это недоразумение, напишите
            нам письмо.
            </div>
        </el-alert>
        <el-alert
                v-if="errorActive"
                :title="errorTitle"
                type="error"
                :description="errorDetails"
                show-icon
                @close="clearError">
        </el-alert>
        <el-form v-if="authState === 0" ref="form" :model="form" :rules="validationRules" label-width="20em" v-loading="isLoading" size="medium">
            <el-form-item>
                <p>Здесь можно регистрировать новые нарушения, <b>совершенные в 2019 году</b>. Они сразу же записываются в базу данных и становятся
                    публично доступны. Не забывайте <b>замазывать персональные данные</b> в прикрепляемых документах.</p>
            </el-form-item>
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
                <ul class="help">
                    <li v-if="!uikMembersLoaded">Выберите комиссию</li>
                    <li v-if="uikMembersLoaded">Если ваш герой в списке отсутствует, напишите его Фамилию Имя Отчество и нажмите Enter</li>
                    <li v-if="uikMembersLoaded">Вы можете выбрать нескольких героев. Тогда новое нарушение будет проассоциировано с каждым из них</li>
                </ul>
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
                            :value="item.value"
                            :title="item.label">
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
                <el-input v-model="link.title" class="link-title" placeholder="Название ссылки"></el-input>
                <el-input v-model="link.url" class="link-url" :id="'crimeLink' + index"
                          type="url"
                          required
                          placeholder="https://example.com"></el-input><span class="validity"></span>

                <el-link icon="el-icon-delete-solid" @click.prevent="removeLink(link)" tabindex="-1">Удалить эту ссылку</el-link>
            </el-form-item>

            <el-form-item>
                <el-button icon="el-icon-circle-plus"
                           @click="addLink"
                           size="mini" round>Добавить ещё одну ссылку</el-button>
            </el-form-item>

            <el-form-item>
                <el-button type="primary" @click="onSubmit" :disabled="!submitEnabled" id="btn-submit-crime">Создать запись</el-button>
            </el-form-item>
            <el-form-item>
                <el-alert id="alert-new-members"
                          v-if="newUikMembers.length > 0"
                          :title="'Мы создадим этих героев как членов ' + selectedUik"
                          type="info"
                          show-icon>
                    <el-row v-for="it in newUikMembers">
                        <el-col :span="2" class="member-status">
                            <el-tag type="danger">{{formatStatus(it.status)}}</el-tag>
                        </el-col>
                        <el-col :span="22">{{it.name}}</el-col>
                    </el-row>
                </el-alert>
            </el-form-item>
            <el-form-item>
                <el-alert
                        v-if="successActive"
                        :title="successTitle"
                        type="success"
                        :description="successDetails"
                        show-icon
                        @close="clearSuccess">
                </el-alert>
                <p>uik={{form.uik}} member={{form.uikMembers}} crime={{form.crimeType}} links={{form.links}}</p>
            </el-form-item>
        </el-form>
    </div>
</template>
<script lang="ts">
  import {Component, Prop, Vue, Watch} from "vue-property-decorator";
  import axios, {AxiosError} from "axios";
  import {
    AllUiksQuery,
    AllUiksResponse, AllUiksResponseItem, CreateCrimeRequest, CreateCrimeResponse,
    FilterData, formatStatus as fmtStatus, formatUikLabel,
    UikMembersQuery,
    UikMembersResponse, UikMemberDto,
    UikType
  } from "@/components/Model";
  import {initFirebase} from "@/AuthInit";
  import * as firebase from "firebase";

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
    reset(): void;
    uik?: number;
    uikMembers?: Array<number | string>;
    crimeType?: string;
    links: Array<LinkItem>;
  }

  enum AuthState { OK, NOT_AUTHENTICATED, UNKNOWN, NEED_EMAIL_CONFIRMATION, FORBIDDEN }
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
      links: [{title: "", url: "https://"}],
      reset(): void {
        this.links = [{title: "", url: "https://"}];
        this.uik = undefined;
        this.uikMembers = undefined;
        this.crimeType = undefined;
      }
    };

    private validationRules = {
        crimeLink: [
          {validator: this.validateCrimeLink, trigger: 'blur'}
        ]
    };

    private authState: AuthState = AuthState.UNKNOWN;
    private userName = "";
    private userEmail = "";
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
      this.login();
      this.loadUiks({
        year: 2019
      });
      this.loadCrimeTypes();
      this.focusUikChooser();
    }

    private focusUikChooser() {
      window.setTimeout(() => {
        const input = document.querySelector("#uik-chooser") as HTMLInputElement;
        if (input) {
          input.focus();
        }
      }, 1000);
    }

    login() {
      initFirebase();
      firebase.auth().onAuthStateChanged(user => {
        console.dir(user);
        if (user && user.email) {
          this.userName = user.displayName || "";
          this.userEmail = user.email;
          if (user.emailVerified) {
            const data = `email=${encodeURIComponent(user.email)}`;
            axios({
              url: "/_ah/signin/do",
              method: "POST",
              data: data,
              headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8'}
            }).then(response => {
              if (response.status === 200) {
                this.loadAuthState();
              } else {
                this.errorActive = true;
                this.errorTitle = "Что-то пошло не так";
                this.errorDetails = response.statusText;
              }
            }).catch((error: AxiosError) => {
                if (error.response && error.response.status === 404) {
                  this.authState = AuthState.FORBIDDEN;
                } else {
                  this.handleError(error);
                }
            });
          } else {
            user.sendEmailVerification().then(() => {
              this.authState = AuthState.NEED_EMAIL_CONFIRMATION;
            }).catch(error => {
              this.authState = AuthState.UNKNOWN;
              this.isLoading = false;
              this.errorActive = true;
              this.errorTitle = "Ошибка при аутентификации";
              this.errorDetails = error;
            });
          }
        } else {
          window.location.href = `/login?redirect=${encodeURIComponent('/edit')}`;
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

    signOut() {
      firebase.auth().signOut().then(() => {
        this.close();
      }).catch(error => {
        this.authState = AuthState.UNKNOWN;
        this.isLoading = false;
        this.errorActive = true;
        this.errorTitle = "Ошибка при выходе";
        this.errorDetails = error;
      });
    }
    close() {
      //window.location.pathname = "/";
    }
    handleClose() {
      //window.location.pathname = "/";
    }

    get newUikMembers(): Array<UikMemberDto> {
      if (this.form.uikMembers === undefined) {
        return [];
      }
      let result: Array<UikMemberDto> = [];
      const names = this.form.uikMembers.filter(it => typeof it === "string") as Array<string>;
      names.forEach(it => {
        const lower = it.toLowerCase();
        let status = 4;
        if (lower.startsWith("пред.")) {
          status = 1;
          it = it.slice(5).trim();
        } else if (lower.startsWith("зам.")) {
          status = 2;
          it = it.slice(4).trim();
        } else if (lower.startsWith("секр.")) {
          status = 3;
          it = it.slice(5).trim();
        }
        result.push({
          id: -1,
          name: it,
          status: status
        })
      });
      return result;
    }

    get selectedUik(): string {
      if (this.form.uik === undefined) {
        return "";
      }
      const item = this.allUiks.filter(it => it.value === this.form.uik);
      return item.length > 0 ? item[0].label : "";
    }

    onSubmit() {
      if (this.form.uik === undefined || this.form.uikMembers === undefined || this.form.crimeType === undefined) {
        return;
      }
      const query: CreateCrimeRequest = {
        uik: this.form.uik,
        uikMembers: this.form.uikMembers.filter(it => typeof it === "number") as Array<number>,
        newUikMembers: this.newUikMembers,
        crimeType: this.form.crimeType,
        crimeLinks: this.form.links
      };
      this.isLoading = true;
      this.httpClient.post<CreateCrimeResponse>("/_ah/api/blacklist/v1/create_crime", query).then(response => {
        if (response.data.crimeId > 0) {
            this.successTitle = "Нарушение опубликовано";
            this.successDetails = response.data.message;
            this.successActive = true;
            window.setInterval(() => this.clearSuccess(), 10000);
            this.form.reset();
            this.focusUikChooser();
        } else {
          this.errorTitle = "Что-то пошло не так";
          this.errorDetails = `Публикация нарушения завершилась с ошибкой: ${response.data.message}`
          this.errorActive = true;
        }
        this.isLoading = false;
      }).catch(this.handleError);
    }

    addLink() {
      this.form.links.push({title: "", url: "https://"})
    }
    removeLink(link: LinkItem) {
      this.form.links = this.form.links.filter(item => item !== link);
    }

    @Watch("visible")
    onVisibleChange() {
    }

    @Watch("form.uik")
    onUikNumberChange() {
      this.form.uikMembers = [];
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
          && this.form.links.filter(it => isUrl(it.url)).length == this.form.links.length;
    }

    private loadUiks(query: AllUiksQuery) {
      this.isLoading = true;
      this.httpClient.post<AllUiksResponse>("/_ah/api/blacklist/v1/all_uiks", query).then(response => {
        this.allUiks = response.data.uiks.map(item => {
          return {
            value: item.id,
            label: formatUikLabel(item),
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
                label: `${fmtStatus(item.status)} ${item.name}`
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

    private formatStatus = fmtStatus;
  }

  function isUrl(url: string) {
    url = url.trim();
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      return false;
    }
    const posHostStart = url.indexOf("://") + 3;
    let posHostEnd = url.indexOf("/", posHostStart);
    if (posHostEnd == -1) posHostEnd = url.length;
    const host = url.slice(posHostStart, posHostEnd);
    return host.indexOf(".") > 0;
  }
</script>
<style lang="scss">
    @import '../assets/style/theme';
    .el-dialog__title {
        font-size: xx-large !important;
    }
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
    ul.help {
        line-height: normal;
        opacity: 0.8;
    }
    #alert-new-members {
        .el-alert__content {
            width: 100%;
            .member-status {
                padding-right: 0.5em;
                text-align: right;
                font-weight: bold;
            }
        }
    }
    #btn-submit-crime {
        background: transparent;
        opacity: 0.5;
        &.is-disabled {
            opacity: 0.25;
        }
        &:hover {
            opacity: 1.0;
        }
        &:focus {
            opacity: 1.0;
        }

    }
    div.header {
        padding: 40px;
        display: flex;
        justify-content: space-between;
    }


    .logo-row {
        display: flex;
        img {
            margin-left: 20px;
        }
    }

    .header-title {
        div {
            text-align: left;
        }
    }

    .first-title {
        font-family: Lisa;
        font-size: 31px;
        color: $color-brick;
    }

    .second-title {
        font-size: 18px;
        color: black;
        margin-top: 10px;
    }

    .third-title {
        font-size: 12px;
        color: $color-grey;
    }
</style>

