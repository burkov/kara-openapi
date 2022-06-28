import {EmailsControllerApi} from './api/emails'


const main = async () => {
    const api = new EmailsControllerApi()
    await api.listEmails();
    await api.createEmail({});
}

main().catch(console.error)
