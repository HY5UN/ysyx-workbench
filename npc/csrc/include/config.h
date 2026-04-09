// #define ENABLE_DIFFTEST
// #define ENABLE_ITRACE
// #define ENABLE_FTRACE

// #define ENABLE_FST
// #define MAX_SIM_TIME 100000

#define BATCH_MODE

void display_config(){
    printf("Configuration: ");
    #ifdef ENABLE_DIFFTEST
    printf("DIFFTEST ");
    #endif
    #ifdef ENABLE_ITRACE
    printf("ITRACE ");
    #endif
    #ifdef ENABLE_FTRACE
    printf("FTRACE ");
    #endif
    #ifdef ENABLE_FST
    printf("FST ");
    #endif
    printf("\n");
    

}