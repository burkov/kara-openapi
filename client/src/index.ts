import {EmailsControllerApi} from "./api/emails";
import {Configuration, UsersControllerApi} from "./api/users";

const main = async () => {
    const emailsApi = new EmailsControllerApi(new Configuration({apiKey: '123'}))
    const usersApi = new UsersControllerApi()


    await emailsApi.updateEmail(1, {e})
}

main().catch(console.error)
