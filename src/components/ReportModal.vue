<template>
  <el-dialog
      title="Дополнить реестр"
      :visible.sync="visible"
      width="50%"
      center
      class="report-modal"
      :before-close="handleClose">
    <el-form :model="formData" :rules="rules" ref="reportForm" label-position="top" class="report-form">
      <el-form-item label="Ваша фамилия, имя и отчество" prop="name">
        <el-input v-model="formData.name" placeholder="Представьтесь, пожалуйста"></el-input>
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Электронная почта" prop="email">
            <el-input v-model="formData.email" placeholder="example@gmail.com"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Телефон" prop="phone">
            <div class="el-input">
              <input class="el-input__inner" @blur="phoneBlur" type="text" v-mask="'+7 (###) ###-##-##'" placeholder="+7 (999) 99-99-99" v-model="formData.phone">
            </div>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="Ваш статус на выборах" prop="status">
            <el-select v-model="formData.status" :popper-append-to-body="false" label="Ваш статус на выборах" placeholder="Не определён">
              <el-option
                  v-for="item in statusData"
                  :key="item"
                  :label="item"
                  :value="item"/>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Номер избирательного участка" prop="uik">
            <el-input v-model.number="formData.uik" placeholder="123"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Номер ТИК">
            <el-input v-model="formData.tik" placeholder="123"></el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="Выберите год" prop="year">
            <el-select v-model="formData.year" :popper-append-to-body="false" label="Выберите год" placeholder="Выберите год">
              <el-option
                  v-for="item in Array.from({length: 10}, (x,i) => i + 2011)"
                  :key="item"
                  :label="item"
                  :value="item"/>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="Город и район" prop="region">
        <el-input v-model="formData.region" placeholder="Укажите город и район"></el-input>
      </el-form-item>
      <el-form-item label="Выберите нарушения" prop="report">
        <el-select v-model="formData.report"
                   multiple
                   collapse-tags
                   :popper-append-to-body="false"
                   placeholder="Выберите нарушения"
                   label="Выберите нарушения">
          <el-option
              v-for="item in reportData"
              :key="item"
              :label="item"
              :value="item"/>
        </el-select>
      </el-form-item>
      <el-form-item label="Член избиркома, совершивший нарушение избирательного законодательства" prop="uik_member">
        <el-input v-model="formData.uik_member" placeholder="Фамилия Имя Отчество"></el-input>
      </el-form-item>
      <el-row>
        <el-col :span="12">
          <el-form-item label="Статус нарушителя" prop="uik_member_status">
            <el-select v-model="formData.uik_member_status"
                       :popper-append-to-body="false"
                       placeholder="Не определён"
                       label="Статус нарушителя">
              <el-option
                  v-for="item in uikMemberStatusData"
                  :key="item"
                  :label="item"
                  :value="item"/>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="Ссылка на материалы, доказывающие нарушение" prop="media">
        <el-input v-model="formData.media" placeholder="Загрузите файл и вставьте ссылку в это поле"></el-input>
      </el-form-item>

        <el-checkbox v-model="agreement">
          <span class="agreement-text">
            Я даю согласие общественной организации «Наблюдатели Петербурга» на обработку указанных в этой форме персональных данных (отозвать согласие можно, написав письмо по адресу
            <a href="mailto:info+blacklist@spbelect.org">info+blacklist@spbelect.org</a>)
          </span>
        </el-checkbox>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button :loading="buttonLoading" :disabled="!agreement" @click="submitForm('reportForm')">Отправить</el-button>
      <div class="notice">Все поля являются обязательными, кроме номера ТИК</div>
    </div>

  </el-dialog>
</template>
<script>
  import axios from "axios"

  const uikMemberStatusData = [
      "Председатель",
      "Секретарь",
      "Заместитель председателя",
      "Член комиссии с ПРГ",
      "Член комиссии с ПСГ",
      "Другой статус"
  ];

  const statusData = [
    "Член этой комиссии с ПРГ",
    "Член этой комиссии с ПСГ",
    "Член вышестоящей комиссии с ПРГ",
    "Член вышестоящей комиссии с ПСГ",
    "Наблюдатель",
    "Журналист",
    "Избиратель",
    "Кандидат",
    "Доверенное лицо кандидата",
    "Иное"
  ];

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
    components: {},
    data() {
      const checkName = (rule, value, callback) => {
        const re = new RegExp("([А-ЯЁ][а-яё]+[\\-\\s]?){3,}");
        if (value === '') {
          callback(new Error('Введите ФИО'));
        }
        if (!re.test(value)) {
          callback(new Error('Введите корректные ФИО'));
        }
        callback()
      };
      const checkPhone = (rule, value, callback) => {
        const phone = value.replace(/\D/g, "");
        const re = new RegExp("79\\d{9}");
        if (phone === '') {
          callback(new Error('Введите номер телефона'));
        }
        if (!re.test(phone)) {
          callback(new Error('Введите корректный номер телефона'));
        }
        callback()
      };
      const checkMedia = (rule, value, callback) => {
        const re = new RegExp("(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&=]*)");
        if (!re.test(value)) {
          callback(new Error('Укажите корректную ссылку на нарушение'));
        }
        callback()
      };
      return {
        uikMemberStatusData: uikMemberStatusData,
        statusData: statusData,
        reportData: reportData,
        buttonLoading: false,
        agreement: false,
        formData: {
          name: "",
          email: "",
          phone: "",
          status: "",
          uik: "",
          tik: "",
          region: null,
          report: [],
          uik_member: "",
          uik_member_status: "",
          media: "",
          year: "",
        },
        rules: {
          name: [
            { validator: checkName, trigger: 'blur' }
          ],
          email: [
            { type: 'email', required: true, message: 'Введите Email', trigger: 'blur' }
          ],
          phone: [
            { validator: checkPhone, trigger: 'blur' }
          ],
          status: [
            { type: 'string', required: true, message: 'Введите статус', trigger: 'change' }
          ],
          uik: [
            { type: 'number', required: true, message: 'Введите номер УИК', trigger: 'blur' }
          ],
          region: [
            { type: 'string', required: true, message: 'Введите город и район', trigger: 'blur' }
          ],
          report: [
            { type: 'array', required: true, message: 'Выберите хотя бы одно значение', trigger: 'change' }
          ],
          uik_member: [
            { type: 'string', required: true, message: 'Введите ФИО нарушителя', trigger: 'blur' }
          ],
          uik_member_status: [
            { type: 'string', required: true, message: 'Введите статус нарушителя', trigger: 'change' }
          ],
          year: [
            { type: 'number', required: true, message: 'Выберите год', trigger: 'change' }
          ],
          media: [
            { validator: checkMedia, trigger: 'blur' }
          ]
        }
      };
    },
    watch: {
    },
    props: ["visible"],
    computed: {},
    methods: {
      close() {
        this.$emit("closeReport");
        this.resetForm()
      },
      handleClose() {
        this.$emit("closeReport");
        this.resetForm()
      },
      submitForm(formName) {
        this.buttonLoading = true;
        this.$refs[formName].validate(valid => {
          const fd = {...this.formData};
          fd.report = fd.report.join(", ");
          if (valid) {
            axios.post('https://registry.tbrd.ru/api.php', fd, {
              params: {
                method: 'add',
              }
            })
            .then(response => {
              if (response) {
                this.buttonLoading = false;
                this.$message({
                  showClose: true,
                  message: 'Спасибо за сообщение!',
                  type: 'success'
                });
                this.close()
              }
            })
            .catch(error => {
              console.log(error);
              this.buttonLoading = false
            });
          } else {
            this.buttonLoading = false;
            return false;
          }
        });
      },
      resetForm() {
        this.$refs.reportForm.resetFields();
        this.agreement = false;
        this.formData.uik_member_status = "";
        this.formData.status = "";
      },
      phoneBlur() {
        this.$refs.reportForm.validateField('phone')
      }
    }
  };
</script>
<style lang="scss" scoped>
  @import '../assets/style/theme';
  .report-modal /deep/ .el-dialog__title {
    font-size: 18px;
    font-weight: bold;
  }

  .report-form /deep/ {
    .el-form-item {
      margin-bottom: 20px !important;
    }
    .el-form-item__label {
      padding-bottom: 5px !important;
      line-height: normal !important;
      color: black !important;
      &::before {
        display: none;
      }
    }
    .el-input__inner, .el-textarea__inner {
      background-color: #ededed !important;
    }
    .el-checkbox {
      display: flex;
    }
    .el-select {
      display: block;
    }
    .agreement-text {
      white-space: normal;
    }
    .el-checkbox__input {
      padding-top: 3px !important;
    }
  }


  button {
    color: $color-brick;
    border-color: $color-brick !important;
    width: 50% !important;
    height: 46px !important;
  }

  .notice {
    margin-top: 20px;
  }
</style>

