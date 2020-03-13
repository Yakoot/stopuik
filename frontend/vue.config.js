module.exports = {
    // options...
    devServer: {
        disableHostCheck: true
    },
    pages: {
      app: {
          entry: './src/main.ts',
          template: './public/index.html',
          filename: 'index.html',
          title: 'Реестр Нарушений'
      } ,
      auth: {
          entry: './src/auth.ts',
          template: './public/auth.html',
          filename: 'auth.html',
          title: 'Реестр Нарушений: Аутентификация'
      }
    }
}
