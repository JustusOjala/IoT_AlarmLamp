#include <stdio.h>
//#include "freertos/FreeRTOS.h"
//#include "freertos/task.h"
#include "driver/gpio.h"
#include "sdkconfig.h"
#include "driver/ledc.h"
#include "driver/adc.h"

#define mos_pin 25

//Struct for control points to control the brightness of the lamp during wakeup
typedef struct {
    float brightness;
    float time;
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

    //Configure ADC
    adc1_config_channel_atten(ADC1_CHANNEL_7, 3); //Potentiometer
    adc1_config_channel_atten(ADC1_CHANNEL_6, 3); //Button
    
    //Current and previous potentiometer reading
    int pot = 0; int prev = 0;

    //Current brightness and whether or not the lamp is on
    int brightness = 0; int on = 0;

    //Values for remote control
    int bezier = 0; //Whether or not the brightness curve is a bezier curve
    int autom = 0;  //Whether or not the brightness is currently determined automatically
    cpoint control_points[4]; //The brightness curve control points
    while(1) {
        //Check the button reading and toggle lamp/override automatic control if high
        if(adc1_get_raw(ADC1_CHANNEL_6) > 2000){
            if(autom = false) on != on;
            else autom = false;
        }

        //Check the potentiometer value, update brightness and override automatic
        //control if altered sufficiently
        pot = adc1_get_raw(ADC1_CHANNEL_7);
        if(abs(pot-prev) >= 10){
            brightness = ((float) pot) / 4095.0f * 8191.0f;
            if(brightness < 0) brightness = 0;
            if(brightness > 8191) brightness = 8191;
            autom = false;
        }

        //Set the PWM value based on the brightness and whether the lamp is on
        if(on){
            ESP_ERROR_CHECK(ledc_set_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0, brightness));
            ESP_ERROR_CHECK(ledc_update_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0));
        }else{
            ESP_ERROR_CHECK(ledc_set_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0, 0));
            ESP_ERROR_CHECK(ledc_update_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0));
        }
    }
}