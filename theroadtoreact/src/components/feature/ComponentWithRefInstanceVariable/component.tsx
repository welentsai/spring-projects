import * as React from 'react';


const ComponentWithRefInstanceVariable = () => {

    const [count, setCount] = React.useState(0);

    const isFirstRender = React.useRef(true);

    function onClick() {
        setCount(count + 1);
    }

    React.useEffect(() => {
        if (isFirstRender.current) {
            console.log('oops')
            isFirstRender.current = false;
        } else {
            console.log(
                `
                I am a useEffect hook's logic
                which runs for a component's
                re-render.
                `
            )
        }
    });

    return (
        <div>
            <p>{count}</p>

            <button
                type='button'
                onClick={onClick}
            >Increase</button>
        </div>
    )
}

export { ComponentWithRefInstanceVariable };