#include <stdio.h>
#include <string.h>
//#include "freertos/FreeRTOS.h"
//#include "freertos/task.h"
#include "driver/gpio.h"
#include "sdkconfig.h"
#include "driver/ledc.h"
#include "driver/adc.h"
#include "esp_timer.h"
#include "esp_wifi.h"
#include "esp_now.h"

#define mos_pin 14
#define button 26

int received = 0;
uint8_t* rdata = NULL;

void onReceive(const uint8_t* MAC, const uint8_t* data, int len){
    if(rdata != NULL) free(rdata);
    rdata = malloc(len);
    memcpy(rdata, data, len);
    received = 1;
}

//Struct for control points to control the brightness of the lamp during wakeup
typedef struct {
    float brightness;   //Brightness in range [0, 1]
    int time;           //Time from start in minutes
}cpoint;

void app_main(void){
    //Initialize LED PWM timer
    ledc_timer_config_t mos_timer = {
        .speed_mode         = LEDC_LOW_SPEED_MODE,
        .timer_num          = LEDC_TIMER_0,
        .duty_resolution    = LEDC_TIMER_13_BIT,
        .freq_hz            = 5000,
        .clk_cfg            = LEDC_AUTO_CLK
    };
    ESP_ERROR_CHECK(ledc_timer_config(&mos_timer));

    //Initialize LED PWM channel
    ledc_channel_config_t mos_channel = {
        .speed_mode     = LEDC_LOW_SPEED_MODE,
        .channel        = LEDC_CHANNEL_0,
        .timer_sel      = LEDC_TIMER_0,
        .intr_type      = LEDC_INTR_DISABLE,
        .gpio_num       = mos_pin,
        .duty           = 0, // Set duty to 0%
        .hpoint         = 0
    };
    ESP_ERROR_CHECK(ledc_channel_config(&mos_channel));

    //configure button pin
    gpio_reset_pin(button);
    gpio_set_direction(button, GPIO_MODE_INPUT);
    gpio_set_pull_mode(button, GPIO_PULLDOWN_ONLY);

    //Configure ADC
    adc1_config_channel_atten(ADC1_CHANNEL_4, 3); //Potentiometer on pin 32

    //Initialize wi-fi
    esp_wifi_init(WIFI_INIT_CONFIG_DEFAULT);
    esp_wifi_set_mode(WIFI_MODE_STA);
    esp_now_init();
    esp_now_register_recv_cb(onReceive);
    
    //Current and previous potentiometer reading
    int pot = 0; int prev = 0;
    
    //Button values
    int button_read = 0; uint64_t bstates = 0; unsigned int state = 0;

    //Current brightness and whether or not the lamp is on
    float brightness = 0.0f; int on = 0;

    //Values for wakeup brightness control
    int bezier = 0; //Whether or not the brightness curve is a bezier curve
    int autom = 0;  //Whether or not the brightness is currently determined automatically
    cpoint control_points[4]; //The brightness curve control points

    //Values for linear interpolation
    float k = 0;    //The slope of the current piece
    float b0 = 0;   //The vertical offset of the piece
    float t0 = 0;   //The horizontal offset of the piece
    unsigned i = 0; //Current first index
    float t1 = 0;   //The stop time of the current piece

    float tStart = 0; //Start time
    while(1) {
        //Take button reading with debouncing
        if(gpio_get_level(button)){
            bstates |= 1 << state;
        }else bstates &= ~(1 << state);
        state = (state + 1) % 64;
        
        //Check the button reading and toggle lamp/override automatic control if high
        if(!button_read && !(~bstates)){
            button_read = 1;
            if(autom == false) on = !on;
            else autom = false;
        }else if(button_read && !bstates){
            button_read = 0;
        }

        //Check the potentiometer value, update brightness and override automatic
        //control if altered sufficiently
        pot = adc1_get_raw(ADC1_CHANNEL_4);
        if(abs(pot-prev) >= 10){
            brightness = (float) pot / 4095.0f;
            if(brightness < 0.0) brightness = 0.0;
            if(brightness > 1.0) brightness = 1.0;
            autom = false;
        }

        if(received){
            
        }

        //Automatic brightness control for wakeup
        if(autom){
            //To avoid overflow errors, remember to reboot at least once every 292 thousand years
            float t = (float) esp_timer_get_time() / 1000000.0f - tStart;
            if(0 /*bezier*/){
                //Only linear interpolation for now
            }else{
                if(t >= t1){
                    i++;
                    if(i < 3){
                        t0 = t1;
                        t1 = 60*control_points[i + 1].time;
                        b0 = control_points[i].brightness;
                        k = (control_points[i + 1].brightness - b0)/(t1 - t0);
                    }else autom = false;
                }else brightness = b0 + (t - t0)*k;
            }
        }

        //Set the PWM value based on the brightness and whether the lamp is on
        if(on){
            ESP_ERROR_CHECK(ledc_set_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0, brightness * 8191.0f));
            ESP_ERROR_CHECK(ledc_update_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0));
        }else{
            ESP_ERROR_CHECK(ledc_set_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0, 0));
            ESP_ERROR_CHECK(ledc_update_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0));
        }
    }
}