#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "driver/gpio.h"
#include "sdkconfig.h"
#include "driver/ledc.h"

#define MOS_pin 25

void app_main(void)
{
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
        .gpio_num       = MOS_pin,
        .duty           = 0, // Set duty to 0%
        .hpoint         = 0
    };
    ESP_ERROR_CHECK(ledc_channel_config(&mos_channel));

    
    while(1) {
        for(int i = 0; i < 8192; i++){
            ESP_ERROR_CHECK(ledc_set_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0, i));
            ESP_ERROR_CHECK(ledc_update_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0));
            vTaskDelay(5 / portTICK_PERIOD_MS);
        }
        for(int i = 8191; i >= 0; i--){
            ESP_ERROR_CHECK(ledc_set_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0, i));
            ESP_ERROR_CHECK(ledc_update_duty(LEDC_LOW_SPEED_MODE, LEDC_CHANNEL_0));
            vTaskDelay(5 / portTICK_PERIOD_MS);
        }
    }
}