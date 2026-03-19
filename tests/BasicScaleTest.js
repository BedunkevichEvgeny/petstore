import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
    vus: 20,
    duration: '300s',
};

export default function () {
    http.get('https://yb-demo-petapp-petservice-eastus.wittyground-275378f1.eastus.azurecontainerapps.io/petstorepetservice/v2/pet/all');
    sleep(1); // Adjust the sleep time as needed
}
